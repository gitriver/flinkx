package com.dtstack.flinkx.util;

import java.io.File;
import java.io.FileInputStream;
import java.net.URLDecoder;
import java.util.Properties;

import org.apache.commons.io.Charsets;
import org.apache.commons.lang.StringUtils;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;

import com.dtstack.flinkx.config.DataTransferConfig;
import com.dtstack.flinkx.options.OptionParser;
import com.dtstack.flinkx.options.Options;

/**
* @Description: TODO(用一句话描述该文件做什么)
* @author jzz
* @date 2019年12月24日
* @version V1.0
*/
public class JSONParseTest {
    private static ObjectMapper objectMapper = new ObjectMapper();
    public static final String TEST_RESOURCE_DIR = "E:/gitcodes/flinkx/flinkx-examples/examples/";

    public static void main(String[] args) throws Exception {
        String jobPath = TEST_RESOURCE_DIR + "stream_to_stream.json";
        String params
            = "-mode local -job " + jobPath
                + " -pluginRoot ./plugins -confProp {\"flink.checkpoint.interval\":60000,\"flink.checkpoint.stateBackend\":\"/data2/flinkx/flink_checkpoint/\"} -s ./flink_checkpoint/0481473685a8e7d22e7bd079d6e5c08c/chk-*";
        String[] arg2 = params.split("\\s+");

        Options options = new OptionParser(arg2).getOptions();
        Properties confProperties = parseConf(options.getConfProp());
        System.out.println(confProperties);
        System.out.println(confProperties.get("flink.checkpoint.stateBackend"));
        System.out.println(options.getS());

        DataTransferConfig config = DataTransferConfig.parse(readJob(new File(options.getJob())));
        System.out.println(config);
    }

    private static Properties parseConf(String confStr) throws Exception {
        System.out.println(confStr);
        if (StringUtils.isEmpty(confStr)) {
            return new Properties();
        }

        confStr = URLDecoder.decode(confStr, Charsets.UTF_8.toString());
        return objectMapper.readValue(confStr, Properties.class);
    }

    private static String readJob(File file) {
        try {
            FileInputStream in = new FileInputStream(file);
            byte[] fileContent = new byte[(int)file.length()];
            in.read(fileContent);
            return new String(fileContent, "UTF-8");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
