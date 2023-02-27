package config.service;

import config.entity.Config;
import config.repository.ConfigRepository;
import edu.fudan.common.util.Response;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.List;


/**
 * @author fdse
 */
@Service
public class ConfigServiceImpl implements ConfigService {

    private Counter get_configs_ErrorCounter;
    private Counter post_configs_ErrorCounter;
    private Counter put_configs_ErrorCounter;
    private Counter delete_configs_configName_ErrorCounter;
    private Counter get_configs_configName_ErrorCounter;

    @Autowired
    private MeterRegistry meterRegistry;

    @PostConstruct
    public void init() {
        Tags tags = Tags.of("service", "ts-config-service");
        meterRegistry.config().commonTags(tags);
        get_configs_ErrorCounter = Counter.builder("request.get.configs.error").register(meterRegistry);
        post_configs_ErrorCounter = Counter.builder("request.post.configs.error").register(meterRegistry);
        put_configs_ErrorCounter = Counter.builder("request.put.configs.error").register(meterRegistry);
        delete_configs_configName_ErrorCounter = Counter.builder("request.delete.configs.configName.error").register(meterRegistry);
        get_configs_configName_ErrorCounter = Counter.builder("request.get.configs.configName.error").register(meterRegistry);
    }

    @Autowired
    ConfigRepository repository;

    private static final Logger logger = LoggerFactory.getLogger(ConfigServiceImpl.class);

    String config0 = "Config ";

    @Override
    public Response create(Config info, HttpHeaders headers) {
        if (repository.findByName(info.getName()) != null) {
            String result = config0 + info.getName() + " already exists.";
            post_configs_ErrorCounter.increment();
            logger.warn("[create][{} already exists][config info: {}]", config0, info.getName());
            return new Response<>(0, result, null);
        } else {
            Config config = new Config(info.getName(), info.getValue(), info.getDescription());
            repository.save(config);
            logger.info("[create][create success][Config: {}]", info);
            return new Response<>(1, "Create success", config);
        }
    }

    @Override
    public Response update(Config info, HttpHeaders headers) {
        if (repository.findByName(info.getName()) == null) {
            String result = config0 + info.getName() + " doesn't exist.";
            logger.warn(result);
            put_configs_ErrorCounter.increment();
            return new Response<>(0, result, null);
        } else {
            Config config = new Config(info.getName(), info.getValue(), info.getDescription());
            repository.save(config);
            logger.info("[update][update success][Config: {}]", config);
            return new Response<>(1, "Update success", config);
        }
    }


    @Override
    public Response query(String name, HttpHeaders headers) {
        Config config = repository.findByName(name);
        if (config == null) {
            get_configs_configName_ErrorCounter.increment();
            logger.warn("[query][Config does not exist][name: {}, message: {}]", name, "No content");
            return new Response<>(0, "No content", null);
        } else {
            logger.info("[query][Query config success][config name: {}]", name);
            return new Response<>(1, "Success", config);
        }
    }

    @Override
    @Transactional
    public Response delete(String name, HttpHeaders headers) {
        Config config = repository.findByName(name);
        if (config == null) {
            String result = config0 + name + " doesn't exist.";
            delete_configs_configName_ErrorCounter.increment();
            logger.warn("[delete][config doesn't exist][config name: {}]", name);
            return new Response<>(0, result, null);
        } else {
            repository.deleteByName(name);
            logger.info("[delete][Config delete success][config name: {}]", name);
            return new Response<>(1, "Delete success", config);
        }
    }

    @Override
    public Response queryAll(HttpHeaders headers) {
        List<Config> configList = repository.findAll();

        if (configList != null && !configList.isEmpty()) {
            logger.info("[queryAll][Query all config success]");
            return new Response<>(1, "Find all  config success", configList);
        } else {
            get_configs_ErrorCounter.increment();
            logger.warn("[queryAll][Query config, No content]");
            return new Response<>(0, "No content", null);
        }
    }
}
