package com.ceadeal.javafxboot;

import cn.hutool.Hutool;
import cn.hutool.core.io.FileUtil;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.alibaba.fastjson.JSONObject;
import org.junit.Test;

import java.io.File;
import java.nio.charset.Charset;
import java.util.List;

/**
 * @author huangHy
 * @description: TODO
 * @date 2022/1/2 15:38
 */
public class EasyTest {

    @Test
    public void test1() {
        String sourceFils = "", appId = "", appSecret = "", appKey = "";
        File file = new File("D:\\pp\\javafx-boot\\config.txt");
        List<String> strings = FileUtil.readLines(file, Charset.defaultCharset());
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

        System.out.println(sourceFils);


    }

    @Test
    public void ss(){

        ExcelWriter writer = ExcelUtil.getWriter("C:\\Users\\huangxp1\\Desktop\\zhixin\\20220102185901.xlsx");

        System.out.println(writer);



    }
}
