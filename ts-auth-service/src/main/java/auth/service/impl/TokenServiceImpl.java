package auth.service.impl;

import auth.constant.InfoConstant;
import auth.dto.BasicAuthDto;
import auth.dto.TokenDto;
import auth.entity.User;
import auth.exception.UserOperationException;
import auth.repository.UserRepository;
import auth.security.jwt.JWTProvider;
import auth.service.TokenService;
import edu.fudan.common.util.Response;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.text.MessageFormat;
import java.util.List;

/**
 * @author fdse
 */
@Service
public class TokenServiceImpl implements TokenService {

    private Counter post_users_login_ErrorCounter;
    @Autowired
    private MeterRegistry meterRegistry;
    @PostConstruct
    public void init() {
        Tags tags = Tags.of("service", "ts-auth-service");
        meterRegistry.config().commonTags(tags);
        post_users_login_ErrorCounter = Counter.builder("request.post.users.login.error").register(meterRegistry);
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(TokenServiceImpl.class);

    @Autowired
    private JWTProvider jwtProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private DiscoveryClient discoveryClient;

    private String getServiceUrl(String serviceName) {
        return "http://" + serviceName;
    }

    @Override
    public Response getToken(BasicAuthDto dto, HttpHeaders headers) throws UserOperationException {
        String username = dto.getUsername();
        String password = dto.getPassword();
        String verifyCode = dto.getVerificationCode();
//        LOGGER.info("LOGIN USER :" + username + " __ " + password + " __ " + verifyCode);
        String verification_code_service_url = getServiceUrl("ts-verification-code-service");
        if (!StringUtils.isEmpty(verifyCode)) {
            HttpEntity requestEntity = new HttpEntity(headers);
            ResponseEntity<Boolean> re = restTemplate.exchange(
                     verification_code_service_url + "/api/v1/verifycode/verify/" + verifyCode,
                    HttpMethod.GET,
                    requestEntity,
                    Boolean.class);
            boolean id = re.getBody();

            // failed code
            if (!id) {
                post_users_login_ErrorCounter.increment();
                LOGGER.info("[getToken][Verification failed][userName: {}]", username);
                return new Response<>(0, "Verification failed.", null);
            }
        }

        // TODO: Session 与 UserID之间的映射

        // verify username and password
        UsernamePasswordAuthenticationToken upat = new UsernamePasswordAuthenticationToken(username, password);
        try {
            authenticationManager.authenticate(upat);
        } catch (AuthenticationException e) {
            post_users_login_ErrorCounter.increment();
            LOGGER.warn("[getToken][Incorrect username or password][username: {}, password: {}]", username, password);
            return new Response<>(0, "Incorrect username or password.", null);
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserOperationException(MessageFormat.format(
                        InfoConstant.USER_NAME_NOT_FOUND_1, username
                )));
        String token = jwtProvider.createToken(user);
        LOGGER.info("[getToken][success][USER TOKEN: {} USER ID: {}]", token, user.getUserId());
        return new Response<>(1, "login success", new TokenDto(user.getUserId(), username, token));
    }
}
