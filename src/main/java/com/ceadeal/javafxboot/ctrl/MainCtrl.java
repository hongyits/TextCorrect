package com.ceadeal.javafxboot.ctrl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.alibaba.fastjson.JSONObject;
import com.ceadeal.javafxboot.util.WebTextCorrection;
import com.google.gson.Gson;
import de.felixroske.jfxsupport.FXMLController;
import javafx.collections.ObservableMap;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;

/**
 * 主界面控制器
 *
 * @author westinyang
 * @date 2019/4/23 2:01
 */
@Slf4j
@FXMLController
public class MainCtrl implements Initializable {

    // 主容器
    public Pane rootPane;

    public Button btnChooseFile;

    public ToggleGroup myToggleGroup;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        log.info("initialize: {}", location.getPath());
    }


    /**
     * 选择文件按钮单机事件
     *
     * @param actionEvent
     */
    public void onBtnChooseFileClick(ActionEvent actionEvent) {
        URL resource = this.getClass().getResource("/");
        String path = resource.getPath();
        String property = System.getProperty("user.dir"); //app目录

        String sourceFils = "", appId = "", appSecret = "", appKey = "";
        String propertiesName = property + File.separator + "config.txt";
        File fileProperties = new File(propertiesName);
        if (!fileProperties.exists()) {
            alertErr(propertiesName + ",配文文件不存在!");
            return;
        }
        List<String> strings = FileUtil.readLines(fileProperties, Charset.defaultCharset());
        for (String str : strings) {
            String[] split = str.split("=");
            if (split[0].equals("sourceFils")) {
                sourceFils = split[1];
            }
            if (split[0].equals("appId")) {
                appId = split[1];
            }
            if (split[0].equals("appSecret")) {
                appSecret = split[1];
            }
            if (split[0].equals("appKey")) {
                appKey = split[1];
            }
        }
        if (StrUtil.isEmpty(sourceFils)) {
            alertErr("sourceFils配置没写!");
            return;
        }


//        Alert alert1 = new Alert(Alert.AlertType.ERROR);
//        alert1.setContentText(property);
//        alert1.show();
        Window window = rootPane.getScene().getWindow();
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File file = directoryChooser.showDialog(window);
        File[] files = file.listFiles();
        Alert alert = null;
        String msg = "";
        if (files.length == 0) {
            alert = new Alert(Alert.AlertType.ERROR);
            msg = "选择的文件夹没有数据";
        } else {
            Toggle selectedToggle = myToggleGroup.getSelectedToggle();
            String type = selectedToggle.toString().split("'")[1];
            ArrayList<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
            for (String str : file.list()) {
                System.out.println("改变前:" + str);
                String corrected_text = "";
                if (type.equals("免费版")) {
                    corrected_text = this.freeStr(str);
                } else {
                    //go 付费版
                    corrected_text = this.kexfStr(str);
                }
                System.out.println("改变后:" + corrected_text);
                Map<String, Object> row = new LinkedHashMap<String, Object>();
                row.put("sourceFileName", str);
                row.put("toFileName", corrected_text);
                rows.add(row);
            }
            //通过工具类创建writer
            String fileName = DateUtil.format(new Date(), "yyyyMMddHHmmss") + ".xlsx";
            String format = String.format("%s\\%s", sourceFils, fileName);
            ExcelWriter writer = ExcelUtil.getWriter(format);
// 合并单元格后的标题行，使用默认标题样式
            writer.merge(1, "TextCorrection");
// 一次性写出内容，使用默认样式，强制输出标题
            writer.write(rows, true);
// 关闭writer，释放内存
            writer.autoSizeColumnAll();
            writer.disableDefaultStyle();
            writer.close();

            alert = new Alert(Alert.AlertType.INFORMATION);
            msg = "清洗成功";
        }
        alert.setContentText(msg);
        alert.show();
    }


    //免费版文本纠正
    public String freeStr(String oldText) {
        String reqUrl = "http://42.193.145.218/corrector_api";
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Content-Type", "application/json");
        headers.put("User-Agent", "PostmanRuntime/7.26.8");
        String resBody = HttpRequest.post(reqUrl).addHeaders(headers).body("\"" + oldText + "\"").execute().body();
        JSONObject resJb = JSONObject.parseObject(resBody);
        String corrected_text = resJb.getString("corrected_text");
        return corrected_text;
    }


    //科大讯飞文本纠正
    public String kexfStr(String oldText) {
        String appId = "c495258c";
        String appSecret = "ZDZhZmNjNTFmZjlmNzI1NTUyMThhMzQy";
        String appKey = "35b4e29a4cc332fae9ca4c54b815caa8";
        //控制台获取appid等信息进行填写
        WebTextCorrection textCorr = new WebTextCorrection(
                "http://api.xf-yun.com/v1/private/s9a87e3ec",
                appKey,
                appSecret,
                oldText,
                appId,
                "s9a87e3ec"
        );
        try {
            String resp = textCorr.doRequest();
            System.out.println("文本纠错返回结果：" + resp);
            Gson json = new Gson();
            WebTextCorrection.ResponseData respData = json.fromJson(resp, WebTextCorrection.ResponseData.class);
            String textBase64 = "";
            if (respData.getPayLoad().getResult() != null) {
                textBase64 = respData.getPayLoad().getResult().getText();
                String text = new String(Base64.getDecoder().decode(textBase64));
                JSONObject resJb = JSONObject.parseObject(text);
                String idm = resJb.getString("idm");
                return idm.split(",")[2];
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";

    }


    public void alertErr(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setContentText(msg);
        alert.show();
    }
}
