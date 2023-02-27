package consignprice.service;

import consignprice.entity.ConsignPrice;
import consignprice.repository.ConsignPriceConfigRepository;
import edu.fudan.common.util.Response;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

/**
 * @author fdse
 */
@Service
public class ConsignPriceServiceImpl implements ConsignPriceService {

    //该服务只返回success

    private Counter get_consignprice_weight_isWithinRegion_ErrorCounter;
    private Counter get_consignprice_price_ErrorCounter;
    private Counter get_consignprice_config_ErrorCounter;
    private Counter post_consignprice_ErrorCounter;

    @Autowired
    private MeterRegistry meterRegistry;

    @PostConstruct
    public void init() {
        Tags tags = Tags.of("service", "ts-consign-price-service");
        meterRegistry.config().commonTags(tags);
        get_consignprice_weight_isWithinRegion_ErrorCounter = Counter.builder("request.get.consignprice.weight.isWithinRegion.error").register(meterRegistry);
        get_consignprice_price_ErrorCounter = Counter.builder("request.get.consignprice.price.error").register(meterRegistry);
        get_consignprice_config_ErrorCounter = Counter.builder("request.get.consignprice.config.error").register(meterRegistry);
        post_consignprice_ErrorCounter = Counter.builder("request.post.consignprice.error").register(meterRegistry);
    }

    @Autowired
    private ConsignPriceConfigRepository repository;

    String success = "Success";

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsignPriceServiceImpl.class);

    @Override
    public Response getPriceByWeightAndRegion(double weight, boolean isWithinRegion, HttpHeaders headers) {
        ConsignPrice priceConfig = repository.findByIndex(0);
        double price = 0;
        double initialPrice = priceConfig.getInitialPrice();
        if (weight <= priceConfig.getInitialWeight()) {
            price = initialPrice;
        } else {
            double extraWeight = weight - priceConfig.getInitialWeight();
            if (isWithinRegion) {
                price = initialPrice + extraWeight * priceConfig.getWithinPrice();
            }else {
                price = initialPrice + extraWeight * priceConfig.getBeyondPrice();
            }
        }
        return new Response<>(1, success, price);
    }

    @Override
    public Response queryPriceInformation(HttpHeaders headers) {
        StringBuilder sb = new StringBuilder();
        ConsignPrice price = repository.findByIndex(0);
        sb.append("The price of weight within ");
        sb.append(price.getInitialWeight());
        sb.append(" is ");
        sb.append(price.getInitialPrice());
        sb.append(". The price of extra weight within the region is ");
        sb.append(price.getWithinPrice());
        sb.append(" and beyond the region is ");
        sb.append(price.getBeyondPrice());
        sb.append("\n");
        return new Response<>(1, success, sb.toString());
    }

    @Override
    public Response createAndModifyPrice(ConsignPrice config, HttpHeaders headers) {
        ConsignPriceServiceImpl.LOGGER.info("[createAndModifyPrice][Create New Price Config]");
        //update price
        ConsignPrice originalConfig;
        if (repository.findByIndex(0) != null) {
            originalConfig = repository.findByIndex(0);
        } else {
            originalConfig = new ConsignPrice();
        }
        originalConfig.setId(config.getId());
        originalConfig.setIndex(0);
        originalConfig.setInitialPrice(config.getInitialPrice());
        originalConfig.setInitialWeight(config.getInitialWeight());
        originalConfig.setWithinPrice(config.getWithinPrice());
        originalConfig.setBeyondPrice(config.getBeyondPrice());
        repository.save(originalConfig);
        return new Response<>(1, success, originalConfig);
    }

    @Override
    public Response getPriceConfig(HttpHeaders headers) {
        return new Response<>(1, success, repository.findByIndex(0));
    }
}
