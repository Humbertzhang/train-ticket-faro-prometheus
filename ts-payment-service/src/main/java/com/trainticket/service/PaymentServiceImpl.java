package com.trainticket.service;

import com.trainticket.entity.Money;
import com.trainticket.entity.Payment;
import com.trainticket.repository.AddMoneyRepository;
import com.trainticket.repository.PaymentRepository;
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
import java.util.List;
import java.util.Optional;

/**
 * @author  Administrator
 * @date 2017/6/23.
 */
@Service
public class PaymentServiceImpl implements PaymentService{

    private Counter post_payment_ErrorCounter;
    private Counter post_payment_money_ErrorCounter;
    private Counter get_payment_ErrorCounter;

    @Autowired
    private MeterRegistry meterRegistry;

    @PostConstruct
    public void init() {
        Tags tags = Tags.of("service", "ts-payment-service");
        meterRegistry.config().commonTags(tags);
        post_payment_ErrorCounter = Counter.builder("request.post.payment.error").register(meterRegistry);
        post_payment_money_ErrorCounter = Counter.builder("request.post.payment.money.error").register(meterRegistry);
        get_payment_ErrorCounter = Counter.builder("request.get.payment.error").register(meterRegistry);
    }


    @Autowired
    PaymentRepository paymentRepository;

    @Autowired
    AddMoneyRepository addMoneyRepository;

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentServiceImpl.class);

    @Override
    public Response pay(Payment info, HttpHeaders headers){

        if(paymentRepository.findByOrderId(info.getOrderId()) == null){
            Payment payment = new Payment();
            payment.setOrderId(info.getOrderId());
            payment.setPrice(info.getPrice());
            payment.setUserId(info.getUserId());
            paymentRepository.save(payment);
            return new Response<>(1, "Pay Success", null);
        }else{
            post_payment_ErrorCounter.increment();
            PaymentServiceImpl.LOGGER.warn("[pay][Pay Failed][Order not found with order id][PaymentId: {}, OrderId: {}]",info.getId(),info.getOrderId());
            return new Response<>(0, "Pay Failed, order not found with order id" +info.getOrderId(), null);
        }
    }

    @Override
    public Response addMoney(Payment info, HttpHeaders headers){
        Money addMoney = new Money();
        addMoney.setUserId(info.getUserId());
        addMoney.setMoney(info.getPrice());
        addMoneyRepository.save(addMoney);
        return new Response<>(1,"Add Money Success", addMoney);
    }

    @Override
    public Response query(HttpHeaders headers){
        List<Payment> payments = paymentRepository.findAll();
        if(payments!= null && !payments.isEmpty()){
            PaymentServiceImpl.LOGGER.info("[query][Find all payment Success][size:{}]",payments.size());
            return new Response<>(1,"Query Success",  payments);
        }else {
            get_payment_ErrorCounter.increment();
            PaymentServiceImpl.LOGGER.warn("[query][Find all payment warn][{}]","No content");
            return new Response<>(0, "No Content", null);
        }
    }

    @Override
    public void initPayment(Payment payment, HttpHeaders headers){
        Optional<Payment> paymentTemp = paymentRepository.findById(payment.getId());
        if(!paymentTemp.isPresent()){
            paymentRepository.save(payment);
            PaymentServiceImpl.LOGGER.error("[initPayment][Init payment error][Payment not found][PaymentId: {}]",payment.getId());
        }else{
            PaymentServiceImpl.LOGGER.info("[initPayment][Init Payment Already Exists][PaymentId: {}]", payment.getId());
        }
    }
}
