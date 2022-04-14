package com.jphaugla.controller;

import java.text.ParseException;
import java.util.*;
import java.util.concurrent.ExecutionException;

import com.jphaugla.domain.*;

import com.jphaugla.service.Messaging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.web.bind.annotation.*;

import com.jphaugla.service.BankService;


@RestController
public class BankingController {

	@Autowired
	private BankService bankService = BankService.getInstance();
	@Autowired
	private Messaging messaging;

	private static final Logger logger = LoggerFactory.getLogger(BankingController.class);

	// customer
	@RequestMapping("/save_customer")
	public String saveCustomer() throws ParseException {
		bankService.saveSampleCustomer();
		return "Done";
	}


	//  account
	@RequestMapping("/save_account")
	public String saveAccount() throws ParseException {
		bankService.saveSampleAccount();
		return "Done";
	}

	//  transaction
	@RequestMapping("/save_transaction")
	public String saveTransaction() throws ParseException {
		bankService.saveSampleTransaction();
		return "Done";
	}

	@GetMapping("/generateData")
	@ResponseBody
	public String generateData (@RequestParam Integer noOfCustomers, @RequestParam Integer noOfTransactions,
								@RequestParam Integer noOfDays, @RequestParam String key_suffix,
								@RequestParam Boolean pipelined)
			throws ParseException, ExecutionException, InterruptedException, IllegalAccessException {

		bankService.generateData(noOfCustomers, noOfTransactions, noOfDays, key_suffix, pipelined);

		return "Done";
	}

	@GetMapping("/testPipeline")
	@ResponseBody
	public String testPipeline (@RequestParam Integer noOfRecords)
			throws ParseException, ExecutionException, InterruptedException, IllegalAccessException {

		bankService.testPipeline(noOfRecords);

		return "Done";
	}

	@GetMapping("/getTransaction")
	public Transaction getTransaction(@RequestParam String transactionID) {
		Transaction transaction = bankService.getTransaction(transactionID);
		return transaction;
	}


	@GetMapping("/customer")
	public Optional<Customer> getCustomer(@RequestParam String customerId) {
		return bankService.getCustomer(customerId);
	}

	@GetMapping("/deleteCustomerEmail")
	public int deleteCustomerEmail(@RequestParam String customerId) {
		return bankService.deleteCustomerEmail(customerId);
	}

	@PostMapping(value = "/postCustomer", consumes = "application/json", produces = "application/json")
	public String postCustomer(@RequestBody Customer customer ) throws ParseException {
		bankService.postCustomer(customer);
		return "Done\n";
	}

	@PostMapping(value = "/postSecurityKey", consumes = "application/json", produces = "application/json")
	public RecordId postCustomer(@RequestBody User user ) throws ParseException {
		logger.info("in BankingController.postSecurityKey with username and password " + user.getUsername() + "/" + user.getPassword());
		RecordId recordId = messaging.postSecurityKey(user.getUsername(), user.getPassword());
		return recordId;
	}
	@GetMapping("/getSecurityKey")
	public String getPassword (@RequestParam String username, @RequestParam Integer number_instances) {
		logger.info("in BankingController.getSecurityKey with username " + username + " number instances " + number_instances);
		return messaging.getPassword(username, number_instances);
	}

	@GetMapping("/startConnect")
	public void startLoop() throws InterruptedException {
		bankService.startRedisWrite();
	}

	@GetMapping("/switchRedis")
	public void switchRedis() throws InterruptedException {
		bankService.switchTemplate("key1", "key1");
	}

}
