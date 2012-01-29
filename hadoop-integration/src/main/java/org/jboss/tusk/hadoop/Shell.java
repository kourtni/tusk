package org.jboss.tusk.hadoop;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;

import org.jboss.tusk.hadoop.service.MessagePersister;


public class Shell {
	
	private static MessagePersister mp = new MessagePersister();
	
	private static Random random = new Random();
	private static String[] diseases = {"measles", "mumps", "asthma", 
			"malaria", "cancer", "heart disease", "tb", "cooties", 
			"diabetes", "anemia", "arthritis", "chicken pox"};
	private static String[] states = {"AL", "AK", "AZ", "AR", "CA", "CO", "CT", "DE", "DC", "FL",
		"GA", "HI", "ID", "IL", "IN", "IA", "KS", "KY", "LA", "ME",
		"MD", "MA", "MI", "MN", "MS", "MO", "MT", "NE", "NV", "NH",
		"NJ", "NY", "NC", "ND", "OH", "OK", "OR", "PA", "RI", "SC",
		"SD", "TN", "TX", "UT", "VT", "VA", "WA", "WV", "WI", "WY"};
	
	public static void main(String[] args) throws Exception {
		if (args.length < 1) {
			System.err.println("Must have at least one argument, the operation. Usage: ");
			System.err.println("  Shell <operation> <numMessages> <dataSize>");
			System.err.println("    <operation> - 'add' or 'remove' (required)");
			System.err.println("    <operation> - number of messages");
			System.err.println("    <operation> - size in bytes of autogenerated data fields");
			return;
		}
		
		int numMessages = 1;
		int dataSize = 10240;
		
		String operation = args[0];
		
		if (args.length > 1) {
			numMessages = new Integer(args[1]);
		}
		if (args.length > 2) {
			dataSize = new Integer(args[2]);
		}
		
		System.out.println("Running with these options:");
		System.out.println("  operation=" + operation);
		System.out.println("  numMessages=" + numMessages);
		System.out.println("  dataSize=" + dataSize);
		
		if ("add".equals(operation)) {
			for (int i = 0; i < numMessages; i++) {
				writeMessage(i, dataSize);
			}
			System.out.println("Wrote " + numMessages + " messages " +
			"(with corresponding index entries) to HBase.");
		} else if ("remove".equals(operation)) {
			for (int i = 0; i < numMessages; i++) {
				removeMessage(i);
			}
			System.out.println("Removed " + numMessages + " messages " +
			"(and corresponding index entries) from HBase.");
		} else {
			System.err.println("Unknown operation.");
		}
	}

	private static void writeMessage(int i, int dataSize) throws Exception {
		String messageKey = "message-" + i;
		String patientId = "patient-" + randomInt(10000);
		String planId = "plan-" + randomInt(10);
		String groupId = "group-" + randomInt(100);
		String state = randomState();
		String disease1 = randomDisease();
		String disease2 = randomDisease();
		String disease3 = randomDisease();

		Map<String, Object> metadataFields = new HashMap<String, Object>();
		metadataFields.put("messageKey", messageKey);
		metadataFields.put("patientId", patientId);
		metadataFields.put("planId", planId);
		metadataFields.put("groupId", groupId);
		metadataFields.put("state", state);
		metadataFields.put("diseases", disease1 + ", " + disease2 + ", " + disease3);
		
		String messageStr = "<message>" +
				"<key>" + messageKey + "</key>" +
				"<data>" + randomString(dataSize) + "</data>" +
				"<patientId>" + patientId + "</patientId>" +
				"<planId>" + planId + "</planId>" +
				"<groupId>" + groupId + "</groupId>" +
				"<state>" + state + "</state>" +
				"<balance>" + randomInt(10000) + "</balance>" +
				"<medicalHistory>" +
					"<disease>" + disease1 + "</disease>" +
					"<disease>" + disease2 + "</disease>" +
					"<disease>" + disease3 + "</disease>" +
				"</medicalHistory>" +
			"</message>";
		
		byte[] data = messageStr.getBytes();

		mp.writeMessage(messageKey, data);
		mp.writeMessageIndex(messageKey, metadataFields);
		
		byte[] result = mp.readMessage(messageKey);
		if (!Arrays.equals(result, data)) {
			System.out.println("Data read after insert did not match up.");
		}
		
		Map<String, byte[]> indexFields = mp.readMessageIndex(messageKey);
		for (Entry<String, byte[]> field : indexFields.entrySet()) {
			if (!Arrays.equals(metadataFields.get(field.getKey()).toString().getBytes(), field.getValue())) {
				System.out.println("*** Something bad happened. The metadata read from HBase does not match " +
						"the metadata written to HBase.");
			}
		}
		
		//clean up what we inserted; uncomment if you don't want the data sticking around
//		hbs.removeMessageIndex(messageKey);
//		hbs.removeMessage(messageKey);
	}
	
	private static void removeMessage(int i) throws Exception {
		mp.removeMessage("message-" + i);
		mp.removeMessageIndex("message-" + i);
	}

	private static String randomString(int dataSize) {
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < dataSize; i++) {
			buf.append(Character.toChars(randomInt(25) + 97));
		}
		return buf.toString();
	}

	private static int randomInt(int i) {		
		return random.nextInt(i);
	}
	
	private static String randomDisease() {
		return diseases[randomInt(diseases.length)];
	}
	
	private static String randomState() {
		return states[randomInt(diseases.length)];
	}

}