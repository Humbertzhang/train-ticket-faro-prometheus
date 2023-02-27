package trainFood.service;

import edu.fudan.common.util.Response;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import trainFood.entity.*;
import trainFood.repository.TrainFoodRepository;

import javax.annotation.PostConstruct;
import java.util.List;

@Service
public class TrainFoodServiceImpl implements TrainFoodService{

    private Counter get_trainfoods_ErrorCounter;
    private Counter get_trainfoods_tripId_ErrorCounter;

    @Autowired
    private MeterRegistry meterRegistry;

    @PostConstruct
    public void init() {
        Tags tags = Tags.of("service", "ts-train-food-service");
        meterRegistry.config().commonTags(tags);
        get_trainfoods_ErrorCounter = Counter.builder("request.get.trainfoods.error").register(meterRegistry);
        get_trainfoods_tripId_ErrorCounter = Counter.builder("request.get.trainfoods.tripId.error").register(meterRegistry);
    }


    @Autowired
    TrainFoodRepository trainFoodRepository;

    private static final Logger LOGGER = LoggerFactory.getLogger(TrainFoodServiceImpl.class);

    String success = "Success";
    String noContent = "No content";

    @Override
    public TrainFood createTrainFood(TrainFood tf, HttpHeaders headers) {
        TrainFood tfTemp = trainFoodRepository.findByTripId(tf.getTripId());
        if (tfTemp != null) {
            if(tfTemp.getFoodList().equals(tf.getFoodList())){
                TrainFoodServiceImpl.LOGGER.error("[Init TrainFood] Already Exists TripId: {}", tf.getTripId());
            }else{
                tfTemp.setFoodList(tf.getFoodList());
                trainFoodRepository.save(tfTemp);
            }
        } else {
            trainFoodRepository.save(tf);
        }
        return tf;
    }

    @Override
    public Response listTrainFood(HttpHeaders headers) {
        List<TrainFood> trainFoodList = trainFoodRepository.findAll();
        if (trainFoodList != null && !trainFoodList.isEmpty()) {
            return new Response<>(1, success, trainFoodList);
        } else {
            get_trainfoods_ErrorCounter.increment();
            TrainFoodServiceImpl.LOGGER.error("List train food error: {}", noContent);
            return new Response<>(0, noContent, null);
        }
    }

    @Override
    public Response listTrainFoodByTripId(String tripId, HttpHeaders headers) {
        TrainFood tf = trainFoodRepository.findByTripId(tripId);
        if(tf == null){
            get_trainfoods_tripId_ErrorCounter.increment();
            return new Response<>(0, noContent, null);
        }else{
            return new Response<>(1, success, tf.getFoodList());
        }
    }
}
