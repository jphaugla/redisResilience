package com.jphaugla.service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.jphaugla.data.BankGenerator;
import com.jphaugla.domain.*;
import com.jphaugla.repository.*;

import io.lettuce.core.RedisCommandExecutionException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.stereotype.Service;

@Service

public class BankService {

	private static BankService bankService = new BankService();
	@Autowired
	private AsyncService asyncService;

	@Autowired
	private CustomerRepository customerRepository;

	@Autowired
	private PhoneRepository phoneRepository;

	@Autowired
	private EmailRepository emailRepository;

	@Autowired
	private AccountRepository accountRepository;

	@Autowired
	private MerchantRepository merchantRepository;

	@Autowired
	private TransactionRepository transactionRepository;

	@Autowired
	private TransactionReturnRepository transactionReturnRepository;

	@Autowired
	private RedisTemplateRepository redisTemplateRepository;

	private @Value("${spring.redis.testkey}")
	String testKey;

	private @Value("${spring.redis.connectionLoopInterval}")
	int loopInterval;
	private @Value("${spring.redis.waitafterfailover}")
	int waitInOpen;

	private static final Logger logger = LoggerFactory.getLogger(BankService.class);
	private static final String BREAKER_REDIS="breakerRedis";

	private long timerSum = 0;
	private AtomicLong timerCount= new AtomicLong();
	
	public static BankService getInstance(){
		return bankService;		
	}

	public Optional<Customer> getCustomer(String customerId){
		logger.info("in bankservice.getCustomer with ID " + customerId);
		Customer returnCustomer = customerRepository.get(customerId);
		logger.info("returned customer " + returnCustomer);
		return Optional.of(returnCustomer);
	}
	public boolean switchTemplate(String stringKey, String stringValue) throws InterruptedException {
		logger.info("in bankservice switchtemplate");
		Exception exception = null;
		boolean returnval = redisTemplateRepository.switchTemplate(stringKey, stringValue, exception);
		return returnval;
	}

	public void startRedisWrite() throws InterruptedException {
		int loopIndex=0;
		logger.info("in startRedisWrite before loop loopInterval is " + loopInterval + " waitInOpen is " + waitInOpen);
		String testValue;
		do {
			testValue=Integer.toString(loopIndex);
			// logger.info("in write loop with idx " + testValue);
			Boolean failedOver = redisTemplateRepository.testTheWrite(testKey, testValue);
			// logger.info("after the write with failedover " + failedOver);
			loopIndex += 1;
			int waitInterval = loopInterval;
			if (failedOver) {
				logger.info("changing waitInterval to " + waitInOpen);
				waitInterval = waitInOpen;
			}
			sleep(waitInterval);
		} while (true);
	}
	public void saveSampleCustomer() throws ParseException, RedisCommandExecutionException {
		Date create_date = new SimpleDateFormat("yyyy.MM.dd").parse("2020.03.28");
		Date last_update = new SimpleDateFormat("yyyy.MM.dd").parse("2020.03.29");
		String cust = "cust0001";
		Email home_email = new Email("jasonhaugland@gmail.com", "home", cust);
		Email work_email = new Email("jason.haugland@redislabs.com", "work", cust);
		Phone cell_phone = new Phone("612-408-4394", "cell", cust);

		Customer customer = new Customer( cust, "4744 17th av s", "",
				"Home", "N", "Minneapolis", "00",
				"jph", create_date.getTime(), "IDR",
				"A", "BANK", "1949.01.23",
				"Ralph", "Ralph Waldo Emerson", "M",
				"887778989", "SSN", "Emerson", last_update.getTime(),
				"jph", "Waldo",  "MR",
				"help", "MN", "55444", "55444-3322"
		);
		customerRepository.create(customer);
	}

	public Optional<Phone> getPhoneNumber(String phoneString) {
		return phoneRepository.get(phoneString);
	}

	public Optional<Email> getEmail(String email) {
		return Optional.ofNullable(emailRepository.get(email));
	}


	public int deleteCustomerEmail(String customerID) {
		logger.info("in bankservice.deleteCustomerEmail with CustomerID " + customerID);
		int deleteCount = emailRepository.deleteCustomerEmails(customerID);
		return deleteCount;
	}
	public Transaction getTransaction(String transactionID) {
		Optional<Transaction> optionalTransaction;
		Transaction returnTransaction = null;
		optionalTransaction = Optional.ofNullable(transactionRepository.get(transactionID));
		if(optionalTransaction.isPresent()) {
			returnTransaction = optionalTransaction.get();
		}
		return returnTransaction;
	}

	private void sleep(int i) {
		try {
			Thread.sleep(i);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public String getDateFullDayQueryString(String stringDate) throws ParseException {
		Date inDate = new SimpleDateFormat("MM/dd/yyyy").parse(stringDate);
		Long inUnix = inDate.getTime();
		//  since the transaction ID is also in the query can take a larger reach around the date column
		Long startUnix = inUnix - 86400*1000;
		Long endUnix = inUnix + 86400*1000;
		return " @postingDate:[" + startUnix + " " + endUnix + "]";
	}

	public String getDateToFromQueryString(Date startDate, Date endDate) throws ParseException {
		/* Date toDate = new SimpleDateFormat("MM/dd/yyyy").parse(to);
		Date fromDate = new SimpleDateFormat("MM/dd/yyyy").parse(from);
		 */
		Long startUnix = startDate.getTime();
		Long endUnix = endDate.getTime();
		return " @postingDate:[" + startUnix + " " + endUnix + "]";
	}
	//   writeTransaction using crud without future
	private void writeTransaction(Transaction transaction) {
		// logger.info("writing a transaction " + transaction);
		transactionRepository.create(transaction);
	}

	public void postCustomer(Customer customer) {
		logger.info("in postCustomer with Customer =" + customer);
		customerRepository.create(customer);
	}

	public  String generateData(Integer noOfCustomers, Integer noOfTransactions, Integer noOfDays,
								String key_suffix, Boolean pipelined)
			throws ParseException, ExecutionException, InterruptedException, IllegalAccessException, RedisCommandExecutionException {

		List<Account> accounts = createCustomerAccount(noOfCustomers, key_suffix);
		BankGenerator.date = new DateTime().minusDays(noOfDays).withTimeAtStartOfDay();
		BankGenerator.Timer transTimer = new BankGenerator.Timer();

		int totalTransactions = noOfTransactions * noOfDays;

		logger.info("Writing " + totalTransactions + " transactions for " + noOfCustomers
				+ " customers. suffix is " + key_suffix + " Pipelined is " + pipelined);
		int account_size = accounts.size();
		int transactionsPerAccount = noOfDays*noOfTransactions/account_size;
		logger.info("number of accounts generated is " + account_size + " transactionsPerAccount "
				+ transactionsPerAccount);
		List<Merchant> merchants = BankGenerator.createMerchantList();
		List<TransactionReturn> transactionReturns = BankGenerator.createTransactionReturnList();
		merchantRepository.createAll(merchants);
		transactionReturnRepository.createAll(transactionReturns);
		CompletableFuture<Integer> transaction_cntr = null;
		if(pipelined) {
			logger.info("doing this pipelined");
			int transactionIndex = 0;
			List<Transaction> transactionList = new ArrayList<>();
			for(Account account:accounts) {
				for(int i=0; i<transactionsPerAccount; i++) {
					transactionIndex++;
					Transaction randomTransaction = BankGenerator.createRandomTransaction(noOfDays, transactionIndex, account, key_suffix,
							merchants, transactionReturns);
					transactionList.add(randomTransaction);
				}
				transaction_cntr = writeAccountTransactions(transactionList);
				transactionList.clear();
			}

		} else {
			//
			for (int i = 0; i < totalTransactions; i++) {
				//  from the account list, grabbing a random account
				//   with this random cannot do pipelining on account since not in account order
				Account account = accounts.get(new Double(Math.random() * account_size).intValue());
				Transaction randomTransaction = BankGenerator.createRandomTransaction(noOfDays, i, account, key_suffix,
						merchants, transactionReturns);
				transaction_cntr = writeTransactionFuture(randomTransaction);
			}
		}
		transaction_cntr.get();
		transTimer.end();
		logger.info("Finished writing " + totalTransactions + " created in " +
				transTimer.getTimeTakenSeconds() + " seconds.");
		return "Done";
	}
	// writeTransaction using crud with Future
	private CompletableFuture<Integer> writeTransactionFuture(Transaction transaction) throws IllegalAccessException {
		CompletableFuture<Integer> transaction_cntr = null;
		transaction_cntr = asyncService.writeTransaction(transaction);
		//   writes a sorted set to be used as the posted date index

		return transaction_cntr;
	}

	public void saveSampleAccount() throws ParseException, RedisCommandExecutionException {
		Date create_date = new SimpleDateFormat("yyyy.MM.dd").parse("2010.03.28");
		Account account = new Account("cust001", "acct001",
				"credit", "teller", "active",
				"ccnumber666655", create_date.getTime(),
				null, null, null, null);
		accountRepository.create(account);
	}
	public CompletableFuture<Integer>  writeAccountTransactions (List<Transaction> transactionList) throws IllegalAccessException, ExecutionException, InterruptedException {

		CompletableFuture<Integer> returnVal = null;
		returnVal = asyncService.writeAccountTransactions(transactionList);
		return returnVal;
	}
	private  List<Account> createCustomerAccount(int noOfCustomers, String key_suffix) throws ExecutionException, InterruptedException, RedisCommandExecutionException {

		logger.info("Creating " + noOfCustomers + " customers with accounts and suffix " + key_suffix);
		BankGenerator.Timer custTimer = new BankGenerator.Timer();
		List<Account> accounts = null;
		List<Account> allAccounts = new ArrayList<>();
		List<Email> emails = null;
		List<Phone> phoneNumbers = null;
		CompletableFuture<Integer> account_cntr = null;
		CompletableFuture<Integer> customer_cntr = null;
		CompletableFuture<Integer> email_cntr = null;
		CompletableFuture<Integer> phone_cntr = null;
		int totalAccounts = 0;
		int totalEmails = 0;
		int totalPhone = 0;
		// logger.info("before the big for loop");
		for (int i=0; i < noOfCustomers; i++){
			// logger.info("int noOfCustomer for loop i=" + i);
			Customer customer = BankGenerator.createRandomCustomer(key_suffix);
			List<Email> emailList = BankGenerator.createEmail(customer.getCustomerId());
			List<Phone> phoneList = BankGenerator.createPhone(customer.getCustomerId());
			for (Phone phoneNumber : phoneNumbers = phoneList) {
				phone_cntr = asyncService.writePhone(phoneNumber);
			}
			totalPhone = totalPhone + phoneNumbers.size();
			for (Email email: emails = emailList) {
				email_cntr = asyncService.writeEmail(email);
			}
			totalEmails = totalEmails + emails.size();
			accounts = BankGenerator.createRandomAccountsForCustomer(customer, key_suffix);
			totalAccounts = totalAccounts + accounts.size();
			for (Account account: accounts) {
				account_cntr = asyncService.writeAccounts(account);
			}
			customer_cntr = asyncService.writeCustomer(customer);
			if(accounts.size()>0) {
				allAccounts.addAll(accounts);
			}
		}
		logger.info("before the gets");
		account_cntr.get();
		customer_cntr.get();
		email_cntr.get();
		phone_cntr.get();
		custTimer.end();
		logger.info("Customers=" + noOfCustomers + " Accounts=" + totalAccounts +
				" Emails=" + totalEmails + " Phones=" + totalPhone + " in " +
				custTimer.getTimeTakenSeconds() + " secs");
		return allAccounts;
	}
	public void saveSampleTransaction() throws ParseException, RedisCommandExecutionException {
		Date settle_date = new SimpleDateFormat("yyyy.MM.dd").parse("2021.07.28");
		Date post_date = new SimpleDateFormat("yyyy.MM.dd").parse("2021.07.28");
		Date init_date = new SimpleDateFormat("yyyy.MM.dd").parse("2021.07.27");

		Merchant merchant = new Merchant("Cub Foods", "5411",
				"Grocery Stores", "MN", "US");
		logger.info("before save merchant");
		merchantRepository.create(merchant);

		Transaction transaction = new Transaction("1234", "acct01",
				"Debit", merchant.getName() + ":" + "acct01", "referenceKeyType",
				"referenceKeyValue", "323.23",  "323.22", "1631",
				"Test Transaction", init_date.getTime(), settle_date.getTime(), post_date.getTime(),
				"POSTED", null, "ATM665", "Outdoor");
		logger.info("before save transaction");
		writeTransaction(transaction);
	}
	public String testPipeline(Integer noOfRecords) {
		BankGenerator.Timer pipelineTimer = new BankGenerator.Timer();
		redisTemplateRepository.getWriteTemplate().executePipelined(new RedisCallback<Object>() {
			@Override
			public Object doInRedis(RedisConnection connection)
					throws DataAccessException {
				connection.openPipeline();
				String keyAndValue=null;
				for (int index=0;index<noOfRecords;index++) {
					keyAndValue = "Silly"+index;
					connection.set(keyAndValue.getBytes(), keyAndValue.getBytes());
				}
				connection.closePipeline();
				return null;
			}
		});
		pipelineTimer.end();
		logger.info("Finished writing " + noOfRecords + " created in " +
				pipelineTimer.getTimeTakenSeconds() + " seconds.");
		return "Done";
	}


}
