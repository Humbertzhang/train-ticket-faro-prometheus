package assurance.service;

import assurance.entity.*;
import assurance.repository.AssuranceRepository;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * @author fdse
 */
@Service
public class AssuranceServiceImpl implements AssuranceService {

    private Counter get_assurances_ErrorCounter;
    private Counter get_assurances_types_ErrorCounter;
    private Counter delete_assurances_assuranceId_ErrorCounter;
    private Counter delete_assurances_orderId_ErrorCounter;
    private Counter patch_assurances_assuranceId_orderId_typeIndex_ErrorCounter;
    private Counter get_assurances_typeIndex_orderId_ErrorCounter;

    private Counter get_assurances_assuranceid_assuranceId_ErrorCounter;
    private Counter get_assurance_orderid_orderId_ErrorCounter;

    @Autowired
    private MeterRegistry meterRegistry;

    @PostConstruct
    public void init() {
        Tags tags = Tags.of("service", "ts-assurance-service");
        meterRegistry.config().commonTags(tags);
        get_assurances_ErrorCounter = Counter.builder("request.get.assurances.error").register(meterRegistry);
        get_assurances_types_ErrorCounter = Counter.builder("request.get.assurances.types.error").register(meterRegistry);
        delete_assurances_assuranceId_ErrorCounter = Counter.builder("request.delete.assurances.assuranceId.error").register(meterRegistry);
        delete_assurances_orderId_ErrorCounter = Counter.builder("request.delete.assurances.orderId.error").register(meterRegistry);
        patch_assurances_assuranceId_orderId_typeIndex_ErrorCounter = Counter.builder("request.patch.assurances.assuranceId.orderId.typeIndex.error").register(meterRegistry);
        get_assurances_typeIndex_orderId_ErrorCounter = Counter.builder("request.get.assurances.typeIndex.orderId.error").register(meterRegistry);
        get_assurances_assuranceid_assuranceId_ErrorCounter = Counter.builder("request.get.assurances.assuranceid.assuranceId.error").register(meterRegistry);
        get_assurance_orderid_orderId_ErrorCounter = Counter.builder("request.get.assurance.orderid.orderId.error").register(meterRegistry);
    }

    @Autowired
    private AssuranceRepository assuranceRepository;

    private static final Logger LOGGER = LoggerFactory.getLogger(AssuranceServiceImpl.class);

    @Override
    public Response findAssuranceById(UUID id, HttpHeaders headers) {
        Optional<Assurance> assurance = assuranceRepository.findById(id.toString());
        if (assurance == null) {
            get_assurances_assuranceid_assuranceId_ErrorCounter.increment();
            AssuranceServiceImpl.LOGGER.warn("[findAssuranceById][find assurance][No content][assurance id: {}]", id);
            return new Response<>(0, "No Content by this id", null);
        } else {
            AssuranceServiceImpl.LOGGER.info("[findAssuranceById][Find Assurance][assurance id: {}]", id);
            return new Response<>(1, "Find Assurance Success", assurance);
        }
    }

    @Override
    public Response findAssuranceByOrderId(UUID orderId, HttpHeaders headers) {
        Assurance assurance = assuranceRepository.findByOrderId(orderId.toString());
        if (assurance == null) {
            get_assurance_orderid_orderId_ErrorCounter.increment();
            AssuranceServiceImpl.LOGGER.warn("[findAssuranceByOrderId][find assurance][No content][orderId: {}]", orderId);
            return new Response<>(0, "No Content by this orderId", null);
        } else {
            AssuranceServiceImpl.LOGGER.info("[findAssuranceByOrderId][find assurance success][orderId: {}]", orderId);
            return new Response<>(1, "Find Assurance Success", assurance);
        }
    }

    @Override
    public Response create(int typeIndex, String orderId, HttpHeaders headers) {
        Assurance a = assuranceRepository.findByOrderId(orderId);
        AssuranceType at = AssuranceType.getTypeByIndex(typeIndex);
        if (a != null) {
            get_assurances_typeIndex_orderId_ErrorCounter.increment();
            AssuranceServiceImpl.LOGGER.error("[create][AddAssurance Fail][Assurance already exists][typeIndex: {}, orderId: {}]", typeIndex, orderId);
            return new Response<>(0, "Fail.Assurance already exists", null);
        } else if (at == null) {
            get_assurances_typeIndex_orderId_ErrorCounter.increment();
            AssuranceServiceImpl.LOGGER.warn("[create][AddAssurance Fail][Assurance type doesn't exist][typeIndex: {}, orderId: {}]", typeIndex, orderId);
            return new Response<>(0, "Fail.Assurance type doesn't exist", null);
        } else {
            Assurance assurance = new Assurance(UUID.randomUUID().toString(), UUID.fromString(orderId).toString(), at);
            assuranceRepository.save(assurance);
            AssuranceServiceImpl.LOGGER.info("[create][AddAssurance][Success]");
            return new Response<>(1, "Success", assurance);
        }
    }

    @Override
    public Response deleteById(UUID assuranceId, HttpHeaders headers) {
        assuranceRepository.deleteById(assuranceId.toString());
        Optional<Assurance> a = assuranceRepository.findById(assuranceId.toString());
        if (a == null) {
            AssuranceServiceImpl.LOGGER.info("[deleteById][DeleteAssurance success][assuranceId: {}]", assuranceId);
            return new Response<>(1, "Delete Success with Assurance id", null);
        } else {
            delete_assurances_assuranceId_ErrorCounter.increment();
            AssuranceServiceImpl.LOGGER.error("[deleteById][DeleteAssurance Fail][Assurance not clear][assuranceId: {}]", assuranceId);
            return new Response<>(0, "Fail.Assurance not clear", assuranceId);
        }
    }

    @Override
    public Response deleteByOrderId(UUID orderId, HttpHeaders headers) {
        assuranceRepository.removeAssuranceByOrderId(orderId.toString());
        Assurance isExistAssurace = assuranceRepository.findByOrderId(orderId.toString());
        if (isExistAssurace == null) {
            AssuranceServiceImpl.LOGGER.info("[deleteByOrderId][DeleteAssurance Success][orderId: {}]", orderId);
            return new Response<>(1, "Delete Success with Order Id", null);
        } else {
            delete_assurances_orderId_ErrorCounter.increment();
            AssuranceServiceImpl.LOGGER.error("[deleteByOrderId][DeleteAssurance Fail][Assurance not clear][orderId: {}]", orderId);
            return new Response<>(0, "Fail.Assurance not clear", orderId);
        }
    }

    @Override
    public Response modify(String assuranceId, String orderId, int typeIndex, HttpHeaders headers) {
        Response oldAssuranceResponse = findAssuranceById(UUID.fromString(assuranceId), headers);
        Assurance oldAssurance =  ((Optional<Assurance>)oldAssuranceResponse.getData()).get();
        if (oldAssurance == null) {
            patch_assurances_assuranceId_orderId_typeIndex_ErrorCounter.increment();
            AssuranceServiceImpl.LOGGER.error("[modify][ModifyAssurance Fail][Assurance not found][assuranceId: {}, orderId: {}, typeIndex: {}]", assuranceId, orderId, typeIndex);
            return new Response<>(0, "Fail.Assurance not found.", null);
        } else {
            AssuranceType at = AssuranceType.getTypeByIndex(typeIndex);
            if (at != null) {
                oldAssurance.setType(at);
                assuranceRepository.save(oldAssurance);
                AssuranceServiceImpl.LOGGER.info("[modify][ModifyAssurance Success][assuranceId: {}, orderId: {}, typeIndex: {}]", assuranceId, orderId, typeIndex);
                return new Response<>(1, "Modify Success", oldAssurance);
            } else {
                patch_assurances_assuranceId_orderId_typeIndex_ErrorCounter.increment();
                AssuranceServiceImpl.LOGGER.error("[modify][ModifyAssurance Fail][Assurance Type not exist][assuranceId: {}, orderId: {}, typeIndex: {}]", assuranceId, orderId, typeIndex);
                return new Response<>(0, "Assurance Type not exist", null);
            }
        }
    }

    @Override
    public Response getAllAssurances(HttpHeaders headers) {
        List<Assurance> as = assuranceRepository.findAll();
        if (as != null && !as.isEmpty()) {
            ArrayList<PlainAssurance> result = new ArrayList<>();
            for (Assurance a : as) {
                PlainAssurance pa = new PlainAssurance();
                pa.setId(a.getId());
                pa.setOrderId(a.getOrderId());
                pa.setTypeIndex(a.getType().getIndex());
                pa.setTypeName(a.getType().getName());
                pa.setTypePrice(a.getType().getPrice());
                result.add(pa);
            }
            AssuranceServiceImpl.LOGGER.info("[getAllAssurances][find all assurance success][list size: {}]", as.size());
            return new Response<>(1, "Success", result);
        } else {
            get_assurances_ErrorCounter.increment();
            AssuranceServiceImpl.LOGGER.warn("[getAllAssurances][find all assurance][No content]");
            return new Response<>(0, "No Content, Assurance is empty", null);
        }
    }

    @Override
    public Response getAllAssuranceTypes(HttpHeaders headers) {

        List<AssuranceTypeBean> atlist = new ArrayList<>();
        for (AssuranceType at : AssuranceType.values()) {
            AssuranceTypeBean atb = new AssuranceTypeBean();
            atb.setIndex(at.getIndex());
            atb.setName(at.getName());
            atb.setPrice(at.getPrice());
            atlist.add(atb);
        }
        if (!atlist.isEmpty()) {
            AssuranceServiceImpl.LOGGER.info("[getAllAssuranceTypes][find all assurance type success][list size: {}]", atlist.size());
            return new Response<>(1, "Find All Assurance", atlist);
        } else {
            get_assurances_types_ErrorCounter.increment();
            AssuranceServiceImpl.LOGGER.warn("[getAllAssuranceTypes][find all assurance type][No content]");
            return new Response<>(0, "Assurance is Empty", null);
        }
    }
}
