package com.rova.accountService.service;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.google.gson.Gson;
import com.rova.accountService.dto.AccountResponse;
import com.rova.accountService.dto.CreateAccountRequestDto;
import com.rova.accountService.dto.TransactionResponseDto;
import com.rova.accountService.exception.ResourceNotFoundException;
import com.rova.accountService.model.Account;
import com.rova.accountService.model.AccountType;
import com.rova.accountService.model.User;
import com.rova.accountService.repository.AccountRepository;
import com.rova.accountService.repository.UserRepository;
import com.rova.accountService.util.ResponseHelper;
import com.rova.accountService.util.Utility;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;

import com.rova.accountService.http.HttpClient;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.rova.accountService.util.Constants.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {
    private final UserRepository userRepository;

    private final AccountRepository accountRepository;
    private final ResponseHelper responseHelper;
    private final HttpClient httpClient;
    @Value("${transactionServiceUrl}")
    private String transactionServiceUrl;

    @Value("${sqs.transaction.queue.url}")
    private String sqsUrl;

    private final AmazonSQS amazonSQS;

    private final Gson gson;

    @Override
    public AccountResponse createCurrentAccount(CreateAccountRequestDto request){
        try {

            //check if user with customerId exist
            Optional<User> checkUserExist = userRepository.findById(Long.valueOf(request.getCustomerId()));
            if(!checkUserExist.isPresent()){
                log.info("User with Customer ID {} does not exist", request.getCustomerId());
                throw new ResourceNotFoundException("User with Customer ID does not exist");
            }

            Account account = new Account();
            Integer accountNumber = Utility.generateRandomDigits(10);
            account.setAccountNumber(String.valueOf(accountNumber));
            account.setAccountType(AccountType.CURRENT);
            BigDecimal balance = (request.getInitialCredit() == null) ? null : new BigDecimal(request.getInitialCredit());
            account.setBalance(balance);
            account.setCustomerId(request.getCustomerId());
            accountRepository.save(account);

            BigDecimal initialCredit = new BigDecimal(request.getInitialCredit());
            int initialCreditValue = initialCredit.compareTo(BigDecimal.ZERO);

            if(initialCreditValue > 0){
                String url = transactionServiceUrl;
                log.info("URL: {}",url);
                log.info("Making POST request to {}", url);
                String jsonRequest = httpClient.toJson(request);
                log.info("Json Request: {}", jsonRequest);


                SendMessageRequest send_msg_request = new SendMessageRequest()
                        .withQueueUrl(sqsUrl)
                        .withMessageBody(jsonRequest)
                        .withDelaySeconds(5);
                amazonSQS.sendMessage(send_msg_request);
                return responseHelper.getResponse(SUCCESS_CODE, SUCCESS, account, HttpStatus.CREATED);
//                Response response = httpClient.post(getHeader(), jsonRequest, url);
//                String responseBody = response.body().string();
//                log.info("Plain Transaction Response: {}", responseBody);
//                TransactionResponseDto transactionResponseDto = gson.fromJson(responseBody, TransactionResponseDto.class);
//                log.info("Transaction Response: {}", transactionResponseDto);
//                if(transactionResponseDto.getRespCode().equals("00"))
//                    return responseHelper.getResponse(SUCCESS_CODE, SUCCESS, account, HttpStatus.CREATED);
            }
            return responseHelper.getResponse(FAILED_CODE, FAILED, null, HttpStatus.EXPECTATION_FAILED);
        }
        catch (Exception e) {
            return responseHelper.getResponse(FAILED_CODE, FAILED, e.getStackTrace(), HttpStatus.EXPECTATION_FAILED);
        }
    }

    private Map<String, String> getHeader(){
        return Map.of(
                "Content-Type", "application/json; charset=utf-8",
                "Accept", "application/json"
        );
    }
}
