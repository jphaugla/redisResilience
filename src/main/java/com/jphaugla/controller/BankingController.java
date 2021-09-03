package com.jphaugla.controller;

import java.text.ParseException;
import java.util.*;
import java.util.concurrent.ExecutionException;

import com.jphaugla.domain.*;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import com.jphaugla.service.BankService;


@RestController
public class BankingController {

	@Autowired
	private BankService bankService = BankService.getInstance();

	private static final Logger logger = LoggerFactory.getLogger(BankingController.class);
	/*

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

	@GetMapping("/customerByPhone")

	public Customer getCustomerByPhone(@RequestParam String phoneString) {
		logger.debug("In get customerByPhone with phone as " + phoneString);
		return bankService.getCustomerByPhone(phoneString);
	}

	@GetMapping("/customerByEmail")

	public Customer getCustomerByEmail(@RequestParam String email) {
		logger.debug("IN get customerByEmail, email is " + email);
		return bankService.getCustomerByEmail(email);
	}

	@GetMapping("/returned_transactions")

	public List<String> getReturnedTransaction () {
		List<String> returnsCount = new ArrayList<>();
		returnsCount = bankService.getTransactionReturns();
		return returnsCount;
	}

	@GetMapping("/getTransaction")
	public Transaction getTransaction(@RequestParam String transactionID) {
		Transaction transaction = bankService.getTransaction(transactionID);
		return transaction;
	}

	 */
	@GetMapping("/customer")

	public Optional<Customer> getCustomer(@RequestParam String customerId) {
		return bankService.getCustomer(customerId);
	}
	// customer
	@RequestMapping("/save_customer")
	public String saveCustomer() throws ParseException {
		bankService.saveSampleCustomer();
		return "Done";
	}

	@PostMapping(value = "/postCustomer", consumes = "application/json", produces = "application/json")
	public String postCustomer(@RequestBody Customer customer ) throws ParseException {
		bankService.postCustomer(customer);
		return "Done\n";
	}


}
