/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dtstack.flinkx.test;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.apache.flink.api.common.JobExecutionResult;
import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.api.common.time.Time;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.runtime.jobgraph.SavepointRestoreSettings;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.CheckpointConfig;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.transformations.PartitionTransformation;
import org.apache.flink.streaming.runtime.partitioner.DTRebalancePartitioner;
import org.apache.flink.types.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dtstack.flink.api.java.MyLocalStreamEnvironment;
import com.dtstack.flinkx.config.DataTransferConfig;
import com.dtstack.flinkx.constants.ConfigConstrant;
import com.dtstack.flinkx.constants.PluginNameConstrant;
import com.dtstack.flinkx.reader.DataReader;
import com.dtstack.flinkx.stream.reader.StreamReader;
import com.dtstack.flinkx.stream.writer.StreamWriter;
import com.dtstack.flinkx.util.ResultPrintUtil;
import com.dtstack.flinkx.writer.DataWriter;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;

/**
 * @author jiangbo
 */
public class LocalTest {

    public static Logger LOG = LoggerFactory.getLogger(LocalTest.class);

    private static final int FAILURE_RATE = 3;

    private static final int FAILURE_INTERVAL = 6;

    private static final int DELAY_INTERVAL = 10;

    public static final String TEST_RESOURCE_DIR = "E:/gitcodes/flinkx/flinkx-examples/examples/";

    public static Configuration conf = new Configuration();

    public static void main(String[] args) throws Exception{
        setLogLevel(Level.INFO.toString());

        Properties confProperties = new Properties();
        confProperties.put("flink.checkpoint.interval", "10000");
        confProperties.put("flink.checkpoint.stateBackend",
            "file:///E:/gitcodes/flinkx/flinkx-test/tmp/flinkx_checkpoint");

//        conf.setString("metrics.reporter.promgateway.class","org.apache.flink.metrics.prometheus.PrometheusPushGatewayReporter");
//        conf.setString("metrics.reporter.promgateway.host","172.16.10.204");
//        conf.setString("metrics.reporter.promgateway.port","9091");
//        conf.setString("metrics.reporter.promgateway.jobName","108job");
        //                conf.setString("metrics.reporter.promgateway.randomJobNameSuffix","true");
        //                conf.setString("metrics.reporter.promgateway.deleteOnShutdown","true");

        String jobPath = TEST_RESOURCE_DIR + "stream_to_stream.json";
        JobExecutionResult result = LocalTest.runJob(new File(jobPath), confProperties, null);
        ResultPrintUtil.printResult(result);
    }

    private static void setLogLevel(String level){
        LoggerContext loggerContext= (LoggerContext) LoggerFactory.getILoggerFactory();
        //设置全局日志级别
        ch.qos.logback.classic.Logger logger=loggerContext.getLogger("root");
        logger.setLevel(Level.toLevel(level));
    }

    public static JobExecutionResult runJob(File jobFile, Properties confProperties, String savepointPath) throws Exception{
        String jobContent = readJob(jobFile);
        return runJob(jobContent, confProperties, savepointPath);
    }

    public static JobExecutionResult runJob(String job, Properties confProperties, String savepointPath) throws Exception{
        DataTransferConfig config = DataTransferConfig.parse(job);

        MyLocalStreamEnvironment env = new MyLocalStreamEnvironment(conf);

        openCheckpointConf(env, confProperties);

        env.setParallelism(config.getJob().getSetting().getSpeed().getChannel());
        env.setRestartStrategy(RestartStrategies.noRestart());

        DataReader reader = buildDataReader(config, env);
        DataStream<Row> dataStream = reader.readData();

        dataStream = new DataStream<>(dataStream.getExecutionEnvironment(),
                new PartitionTransformation<>(dataStream.getTransformation(),
                        new DTRebalancePartitioner<>()));

        DataWriter writer = buildDataWriter(config);
        writer.writeData(dataStream);

        if(StringUtils.isNotEmpty(savepointPath)){
            env.setSettings(SavepointRestoreSettings.forPath(savepointPath));
        }

        return env.execute();
    }

    private static String readJob(File file) {
        try {
            FileInputStream in = new FileInputStream(file);
            byte[] fileContent = new byte[(int) file.length()];
            in.read(fileContent);
            return new String(fileContent, "UTF-8");
        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    private static DataReader buildDataReader(DataTransferConfig config, StreamExecutionEnvironment env){
        String readerName = config.getJob().getContent().get(0).getReader().getName();
        DataReader reader ;
        switch (readerName){
            case PluginNameConstrant.STREAM_READER : reader = new StreamReader(config, env); break;
            //            case PluginNameConstrant.CARBONDATA_READER : reader = new CarbondataReader(config, env); break;
            //            case PluginNameConstrant.ORACLE_READER : reader = new OracleReader(config, env); break;
            //            case PluginNameConstrant.POSTGRESQL_READER : reader = new PostgresqlReader(config, env); break;
            //            case PluginNameConstrant.SQLSERVER_READER : reader = new SqlserverReader(config, env); break;
            //            case PluginNameConstrant.MYSQLD_READER : reader = new MysqldReader(config, env); break;
            //            case PluginNameConstrant.MYSQL_READER : reader = new MysqlReader(config, env); break;
            //            case PluginNameConstrant.DB2_READER : reader = new Db2Reader(config, env); break;
            //            case PluginNameConstrant.GBASE_READER : reader = new GbaseReader(config, env); break;
            //            case PluginNameConstrant.ES_READER : reader = new EsReader(config, env); break;
            //            case PluginNameConstrant.FTP_READER : reader = new FtpReader(config, env); break;
            //            case PluginNameConstrant.HBASE_READER : reader = new HbaseReader(config, env); break;
            //            case PluginNameConstrant.HDFS_READER : reader = new HdfsReader(config, env); break;
            //            case PluginNameConstrant.MONGODB_READER : reader = new MongodbReader(config, env); break;
            //            case PluginNameConstrant.ODPS_READER : reader = new OdpsReader(config, env); break;
            //            case PluginNameConstrant.BINLOG_READER : reader = new BinlogReader(config, env); break;
            //            case PluginNameConstrant.KAFKA09_READER : reader = new Kafka09Reader(config, env); break;
            //            case PluginNameConstrant.KAFKA10_READER : reader = new Kafka10Reader(config, env); break;
            //            case PluginNameConstrant.KAFKA11_READER : reader = new Kafka11Reader(config, env); break;
            default:throw new IllegalArgumentException("Can not find reader by name:" + readerName);
        }

        return reader;
    }

    private static DataWriter buildDataWriter(DataTransferConfig config){
        String writerName = config.getJob().getContent().get(0).getWriter().getName();
        DataWriter writer;
        switch (writerName){
            case PluginNameConstrant.STREAM_WRITER : writer = new StreamWriter(config); break;
            //            case PluginNameConstrant.CARBONDATA_WRITER : writer = new CarbondataWriter(config); break;
            //            case PluginNameConstrant.MYSQL_WRITER : writer = new MysqlWriter(config); break;
            //            case PluginNameConstrant.SQLSERVER_WRITER : writer = new SqlserverWriter(config); break;
            //            case PluginNameConstrant.ORACLE_WRITER : writer = new OracleWriter(config); break;
            //            case PluginNameConstrant.POSTGRESQL_WRITER : writer = new PostgresqlWriter(config); break;
            //            case PluginNameConstrant.DB2_WRITER : writer = new Db2Writer(config); break;
            //            case PluginNameConstrant.GBASE_WRITER : writer = new GbaseWriter(config); break;
            //            case PluginNameConstrant.ES_WRITER : writer = new EsWriter(config); break;
            //            case PluginNameConstrant.FTP_WRITER : writer = new FtpWriter(config); break;
            //            case PluginNameConstrant.HBASE_WRITER : writer = new HbaseWriter(config); break;
            //            case PluginNameConstrant.HDFS_WRITER : writer = new HdfsWriter(config); break;
            //            case PluginNameConstrant.MONGODB_WRITER : writer = new MongodbWriter(config); break;
            //            case PluginNameConstrant.ODPS_WRITER : writer = new OdpsWriter(config); break;
            //            case PluginNameConstrant.REDIS_WRITER : writer = new RedisWriter(config); break;
            //            case PluginNameConstrant.HIVE_WRITER : writer = new HiveWriter(config); break;
            //            case PluginNameConstrant.KAFKA09_WRITER : writer = new Kafka09Writer(config); break;
            //            case PluginNameConstrant.KAFKA10_WRITER : writer = new Kafka10Writer(config); break;
            //            case PluginNameConstrant.KAFKA11_WRITER : writer = new Kafka11Writer(config); break;
            default:throw new IllegalArgumentException("Can not find writer by name:" + writerName);
        }

        return writer;
    }

    private static void openCheckpointConf(StreamExecutionEnvironment env, Properties properties){
        if(properties == null){
            return;
        }

        if(properties.getProperty(ConfigConstrant.FLINK_CHECKPOINT_INTERVAL_KEY) == null){
            return;
        }else{
            long interval = Long.valueOf(properties.getProperty(ConfigConstrant.FLINK_CHECKPOINT_INTERVAL_KEY).trim());

            //start checkpoint every ${interval}
            env.enableCheckpointing(interval);

            LOG.info("Open checkpoint with interval:" + interval);
        }

        String checkpointTimeoutStr = properties.getProperty(ConfigConstrant.FLINK_CHECKPOINT_TIMEOUT_KEY);
        if(checkpointTimeoutStr != null){
            long checkpointTimeout = Long.valueOf(checkpointTimeoutStr);
            //checkpoints have to complete within one min,or are discard
            env.getCheckpointConfig().setCheckpointTimeout(checkpointTimeout);

            LOG.info("Set checkpoint timeout:" + checkpointTimeout);
        }

        env.getCheckpointConfig().setCheckpointingMode(CheckpointingMode.EXACTLY_ONCE);
        env.getCheckpointConfig().setMaxConcurrentCheckpoints(1);
        env.getCheckpointConfig().enableExternalizedCheckpoints(
                CheckpointConfig.ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION);

        env.setRestartStrategy(RestartStrategies.failureRateRestart(
                FAILURE_RATE,
                Time.of(FAILURE_INTERVAL, TimeUnit.MINUTES),
                Time.of(DELAY_INTERVAL, TimeUnit.SECONDS)
        ));
    }
}
