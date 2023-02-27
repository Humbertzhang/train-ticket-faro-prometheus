package train.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import train.entity.TrainType;
import train.repository.TrainTypeRepository;

import javax.annotation.PostConstruct;
import java.util.List;

@Service
public class TrainServiceImpl implements TrainService {

    @Autowired
    private MeterRegistry meterRegistry;

    private Counter post_trains_ErrorCounter;
    private Counter get_trains_id_ErrorCounter;
    private Counter get_trains_byName_name_ErrorCounter;
    private Counter post_trains_byNames_ErrorCounter;
    private Counter put_trains_ErrorCounter;
    private Counter delete_trains_id_ErrorCounter;
    private Counter get_trains_ErrorCounter;


    @PostConstruct
    public void init() {
        Tags tags = Tags.of("service", "ts-train-service");
        meterRegistry.config().commonTags(tags);

        post_trains_ErrorCounter = Counter.builder("request.post.trains.error").register(meterRegistry);
        get_trains_id_ErrorCounter = Counter.builder("request.get.trains.id.error").register(meterRegistry);
        get_trains_byName_name_ErrorCounter = Counter.builder("request.get.trains.byName.name.error").register(meterRegistry);
        post_trains_byNames_ErrorCounter = Counter.builder("request.post.trains.byNames.error").register(meterRegistry);
        put_trains_ErrorCounter = Counter.builder("request.put.trains.error").register(meterRegistry);
        delete_trains_id_ErrorCounter = Counter.builder("request.delete.trains.id.error").register(meterRegistry);
        get_trains_ErrorCounter = Counter.builder("request.get.trains.error").register(meterRegistry);
    }


    @Autowired
    private TrainTypeRepository repository;

    private static final Logger LOGGER = LoggerFactory.getLogger(TrainServiceImpl.class);

    @Override
    public boolean create(TrainType trainType, HttpHeaders headers) {
        boolean result = false;
        if(trainType.getName().isEmpty()){
            post_trains_ErrorCounter.increment();
            TrainServiceImpl.LOGGER.error("[create][Create train error][Train Type name not specified]");
            return result;
        }
        if (repository.findByName(trainType.getName()) == null) {
            TrainType type = new TrainType(trainType.getName(), trainType.getEconomyClass(), trainType.getConfortClass());
            type.setAverageSpeed(trainType.getAverageSpeed());
            repository.save(type);
            result = true;
        }
        else {
            post_trains_ErrorCounter.increment();
            TrainServiceImpl.LOGGER.error("[create][Create train error][Train already exists][TrainTypeId: {}]",trainType.getId());
        }
        return result;
    }

    @Override
    public TrainType retrieve(String id, HttpHeaders headers) {
        if (!repository.findById(id).isPresent()) {
            get_trains_id_ErrorCounter.increment();
            TrainServiceImpl.LOGGER.error("[retrieve][Retrieve train error][Train not found][TrainTypeId: {}]",id);
            return null;
        } else {
            return repository.findById(id).get();
        }
    }

    @Override
    public TrainType retrieveByName(String name, HttpHeaders headers) {
        TrainType tt = repository.findByName(name);
        if (tt == null) {
            get_trains_byName_name_ErrorCounter.increment();
            TrainServiceImpl.LOGGER.error("[retrieveByName][RetrieveByName error][Train not found][TrainTypeName: {}]", name);
            return null;
        } else {
            return tt;
        }
    }

    @Override
    public List<TrainType> retrieveByNames(List<String> names, HttpHeaders headers) {
        List<TrainType> tt = repository.findByNames(names);
        if (tt == null || tt.isEmpty()) {
            post_trains_byNames_ErrorCounter.increment();
            TrainServiceImpl.LOGGER.error("[retrieveByNames][RetrieveByNames error][Train not found][TrainTypeNames: {}]", names);
            return null;
        } else {
            return tt;
        }
    }

    @Override
    @Transactional
    public boolean update(TrainType trainType, HttpHeaders headers) {
        boolean result = false;
        if (repository.findById(trainType.getId()).isPresent()) {
            TrainType type = new TrainType(trainType.getName(), trainType.getEconomyClass(), trainType.getConfortClass(), trainType.getAverageSpeed());
            type.setId(trainType.getId());
            repository.save(type);
            result = true;
        }
        else {
            put_trains_ErrorCounter.increment();
            TrainServiceImpl.LOGGER.error("[update][Update train error][Train not found][TrainTypeId: {}]",trainType.getId());
        }
        return result;
    }

    @Override
    public boolean delete(String id, HttpHeaders headers) {
        boolean result = false;
        if (repository.findById(id).isPresent()) {
            repository.deleteById(id);
            result = true;
        }
        else {
            delete_trains_id_ErrorCounter.increment();
            TrainServiceImpl.LOGGER.error("[delete][Delete train error][Train not found][TrainTypeId: {}]",id);
        }
        return result;
    }

    @Override
    public List<TrainType> query(HttpHeaders headers) {
        if (repository.findAll().isEmpty()) {
            get_trains_ErrorCounter.increment();
        }
        return repository.findAll();
    }

}
