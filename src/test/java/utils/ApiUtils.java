package utils;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import org.apache.poi.sl.draw.geom.Path;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.XMLUnit;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.simple.JSONArray;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.builder.RequestSpecBuilder;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.http.Method;
import com.jayway.restassured.path.json.JsonPath;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.RequestSpecification;

import junit.framework.Assert;
import resourceManagers.ConfigFileReader;
import resourceManagers.Constants;
import resourceManagers.JsonValidationReporter;


public class ApiUtils  extends JsonValidationReporter{
	//Global Setup Variables
	public static String path;
	public static String jsonPathTerm;
	public static String apiBaseURI;
	public static String apiMethod;
	public static String paraPropNameValue="";
	public static int statusCode;
	public static RestAssured restAssured;
	public static String postResponse;

	static ConfigFileReader configFileReader = new ConfigFileReader();

	public static String readExcel(String testCaseName) throws IOException {
		File src=new File(Constants.expectedTestDataFolderpath + "ConfigurationData.xlsx");
		FileInputStream path=new FileInputStream(src);
		XSSFWorkbook wb= new XSSFWorkbook(path);
		XSSFSheet workSheet = wb.getSheetAt(0);
		int rowCount = workSheet.getPhysicalNumberOfRows();
		String baseURI = null;

		for(int row=1; row<rowCount;row++) {
			Cell value =workSheet.getRow(row).getCell(0);
			//	    		System.out.println(">>>>ValueInCell"+value.getStringCellValue());
			String valueString = value.getStringCellValue();
			if(valueString.equals(testCaseName) && configFileReader.getEnvironment().equals("PreProd")) {
				baseURI =workSheet.getRow(row).getCell(1).getStringCellValue();
				break;

			}
			else if (valueString.equals(testCaseName) &&configFileReader.getEnvironment().equals("QA")) {
				baseURI =workSheet.getRow(row).getCell(2).getStringCellValue();
				break;
			}

		}
		return baseURI;
	}

	//Sets base URI
	public static void setApiBaseURI (String testCaseName) throws IOException{

		apiBaseURI =readExcel(testCaseName);
		System.out.println("baseURL is>>>> "+apiBaseURI);


	}
	
	public static void setApiURI (String BaseURI) throws IOException{

		apiBaseURI =BaseURI;
		System.out.println("baseURL is>>>> "+apiBaseURI);


	}

	//Sets API Method
	public static void setAPIMethod(String APIMethod){
		apiMethod= "/"+APIMethod;
	}

	//Sets API Parameter Name and Value
	public static void setAPIParameter(String ParaName1, String value1 ){
		paraPropNameValue="?"+ParaName1 + "="  + value1;
	}

	public static void setAPIParameter(List<List<String>> parameterList ){
		paraPropNameValue="?";
		for (int ilist=0; ilist<parameterList.size();ilist++)
		{
			paraPropNameValue=paraPropNameValue+parameterList.get(ilist).get(0) + "=" +parameterList.get(ilist).get(1) + "&";
		}
		paraPropNameValue=paraPropNameValue.substring(0, paraPropNameValue.length()-1);
		System.out.println(paraPropNameValue);
	}



	//Returns response
	public static Response getApiResponse() {
		Response res;
		try {
			//System.out.println(apiBaseURI + apiMethod + paraPropNameValue);
			JsonValidationReporter.addStep("URI: "  + apiBaseURI + apiMethod + paraPropNameValue);
			//System.out.println("URI: "  + apiBaseURI + apiMethod + paraPropNameValue);
			res= restAssured.get(apiBaseURI + apiMethod + paraPropNameValue);
			statusCode= res.getStatusCode();
			if (statusCode!=200) {
				assertFalse(true, "API response status code received is " + statusCode );	
			}
		}
		catch(Exception ex) {
			res=null;
		}
		return res;
	}
	
	public static String getHTTPResponse(String BearerToken) throws JSONException, IOException {
		HttpURLConnection conn = null;
		String response = null;
		try {
			

			URL url = new URL(apiBaseURI + apiMethod + paraPropNameValue ); //+ MethodName + "?"
			System.out.println(apiBaseURI + apiMethod + paraPropNameValue);
			conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Content-Type", "application/JSON");
			conn.setRequestProperty("Authorization", BearerToken);  
			conn.connect();
			System.out.println(conn.getResponseCode());
			if (conn.getResponseCode() != 200) {
				if (conn.getResponseCode() != HttpURLConnection.HTTP_CREATED) {    
					throw new RuntimeException("Failed : HTTP error code : "+ conn.getResponseCode());
				}
			}
			else {
				System.out.println(conn.getResponseMessage());
				response=conn.getResponseMessage().toString();
			}
			
		} catch (Exception e) {

			e.printStackTrace();
		}
		return response;
	}


	public static void validateApiResponse(String expectedJsonFilePath) throws JSONException, IOException {


		JSONObject actualJson = new JSONObject(getApiResponse().asString());
		JSONObject expectedJson = parseJSONFile(expectedJsonFilePath);

		int failCount=validateExpectedActualJsonResults(actualJson,expectedJson);
		assertTrue(failCount==0);

	}



	public static int validateExpectedActualJsonResults(JSONObject actualJsonObj,JSONObject expectedJsonObj ) {
		int failCount=0;

		Iterator<String> actualKeyIterator=actualJsonObj.keys();
		Iterator<String> expectedKeyIterator=expectedJsonObj.keys();
		org.json.JSONArray actualJsonArray;
		org.json.JSONArray expectedJsonArray;

		while(actualKeyIterator.hasNext()) {	
			//Geting a key from Json Object
			String actualkey=actualKeyIterator.next().toString();
			String expectedkey=expectedKeyIterator.next().toString();
			System.out.println(actualkey);

			//Casting JSON object into Json Array
			try {
				if (validateIfJasonArray(actualJsonObj, actualkey)) {
					actualJsonArray = actualJsonObj.getJSONArray(actualkey);
					expectedJsonArray = expectedJsonObj.getJSONArray(expectedkey);

					System.out.println(actualJsonArray.length());
					//Traversing on the JASON Array
					for (int i=0; i<actualJsonArray.length(); i++) {
						JSONObject actualChildJsonObj=actualJsonArray.getJSONObject(i);
						JSONObject expectedChildJsonObj=expectedJsonArray.getJSONObject(i);

						Iterator<String> actualChildKeys=actualChildJsonObj.keys();
						Iterator<String> expectedChildKeys=expectedChildJsonObj.keys();

						while(actualChildKeys.hasNext()) {
							String actualKeyName=actualChildKeys.next().toString();
							Object actualkeyValue = actualChildJsonObj.get(actualKeyName);

							String expectedKeyName=expectedChildKeys.next().toString();
							Object expectedkeyValue = expectedChildJsonObj.get(expectedKeyName);

							if (!(actualKeyName.trim().equalsIgnoreCase(expectedKeyName.trim()) && actualkeyValue.equals(expectedkeyValue)) ) {

								failCount=failCount+1;
								JsonValidationReporter.addStep("Expected:-> " + expectedKeyName + " : " + expectedkeyValue.toString() + " Actual :->" + actualKeyName + " : " + actualkeyValue.toString());
							}	
							if (actualChildJsonObj.get(actualKeyName) instanceof JSONObject)
								validateExpectedActualJsonResults((JSONObject)actualkeyValue, (JSONObject)expectedkeyValue);
						}

					}
				}
				//If the key is not a Json Array
				else {

					Object actualkeyValue = actualJsonObj.get(actualkey);
					Object expectedkeyValue = expectedJsonObj.get(expectedkey);

					if (!(actualkey.trim().equalsIgnoreCase(expectedkey.trim()) && actualkeyValue.equals(expectedkeyValue)) ) {

						failCount=failCount+1;
						JsonValidationReporter.addStep("Expected:-> " + expectedkey + " : " + expectedkeyValue.toString() + " Actual :->" + actualkey + " : " + actualkeyValue.toString());
					}
				}
			}
			catch(Exception ex) {
				System.out.println(ex.getMessage());
			}

		}
		return failCount;

	}

	public static int validateExpectedActualJsonResults1(JSONObject actualJsonObj,JSONObject expectedJsonObj ) {
		int failCount=0;

		Iterator<String> actualKeyIterator=actualJsonObj.keys();
		Iterator<String> expectedKeyIterator=expectedJsonObj.keys();
		org.json.JSONArray actualJsonArray;
		org.json.JSONArray expectedJsonArray;

		while(actualKeyIterator.hasNext()) {	
			//Geting a key from Json Object
			String actualkey=actualKeyIterator.next().toString();
			String expectedkey=expectedKeyIterator.next().toString();
			System.out.println(actualkey);

			//Casting JSON object into Json Array

			actualJsonArray = actualJsonObj.getJSONArray(actualkey);
			expectedJsonArray = expectedJsonObj.getJSONArray(expectedkey);

			System.out.println(actualJsonArray.length());
			//Traversing on the JASON Array
			for (int i=0; i<actualJsonArray.length(); i++) {
				JSONObject actualChildJsonObj=actualJsonArray.getJSONObject(i);
				JSONObject expectedChildJsonObj=expectedJsonArray.getJSONObject(i);

				Iterator<String> actualChildKeys=actualChildJsonObj.keys();
				Iterator<String> expectedChildKeys=expectedChildJsonObj.keys();

				while(actualChildKeys.hasNext()) {
					String actualKeyName=actualChildKeys.next().toString();
					Object actualkeyValue = actualChildJsonObj.get(actualKeyName);

					String expectedKeyName=expectedChildKeys.next().toString();
					Object expectedkeyValue = expectedChildJsonObj.get(expectedKeyName);

					if (!(actualKeyName.trim().equalsIgnoreCase(expectedKeyName.trim()) && actualkeyValue.equals(expectedkeyValue)) ) {

						failCount=failCount+1;
						JsonValidationReporter.addStep("Expected:-> " + expectedKeyName + " : " + expectedkeyValue.toString() + " Actual :->" + actualKeyName + " : " + actualkeyValue.toString());
					}	
					if (actualChildJsonObj.get(actualKeyName) instanceof JSONObject)
						validateExpectedActualJsonResults((JSONObject)actualkeyValue, (JSONObject)expectedkeyValue);
				}

			}


		}
		return failCount;

	}


	public static JSONObject parseJSONFile(String filename) throws JSONException, IOException {
		String content = new String(Files.readAllBytes(Paths.get(filename)));
		return new JSONObject(content);
	}



	public static void postHTTPRequest(String FileName) throws JSONException, IOException {
		Response response;
		try {
			String FilePath="";
			if (FileName.contentEquals("AddToCartInputParameter.json"))
				FilePath=Constants.expectedTestDataFolderpath + FileName;

			URL url = new URL(apiBaseURI + apiMethod ); //+ MethodName + "?"
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/json");
			conn.setRequestProperty("Authorization", Constants.bearerToken);  
			String input = parseJSONFile(FilePath).toString();
			OutputStream os = conn.getOutputStream();
			os.write(input.getBytes());
			os.flush();
			conn.connect();
			System.out.println(conn.getResponseCode());
			if (conn.getResponseCode() != 200) {
				if (conn.getResponseCode() != HttpURLConnection.HTTP_CREATED) {    
					throw new RuntimeException("Failed : HTTP error code : "+ conn.getResponseCode());
				}
			}
			else {
				System.out.println(conn.getResponseMessage());

			}
			String type = conn.getContentType();
			if (type == null) {
				return;
			}
		} catch (Exception e) {

			e.printStackTrace();
		}
	}

	public static Response postRequest(String FileName) throws JSONException, IOException {

		//Initializing Rest API's URL
		String APIUrl = apiBaseURI + apiMethod ;
		System.out.println(APIUrl);
		//Initializing payload or API body
		String	FilePath=Constants.inputJSONFolderpath + FileName;
		String APIBody = parseJSONFile(FilePath).toString();

		RequestSpecification requestSpec = RestAssured.with();
		requestSpec.given().contentType("application/json").body(APIBody);
		requestSpec.headers("Authorization", Constants.bearerToken);
		Response response = requestSpec.post(APIUrl);
		
		System.out.println("Status Code: " +response.getStatusCode());

		if (response.getStatusCode() != 200) {
			throw new RuntimeException("Failed : HTTP error code : "+ response.getStatusCode());
		}
		else {
			postResponse=response.body().asString();
		}

		return response;
	}
	
	public static Response postRequestWithParametrInURI(String FileName) throws JSONException, IOException {

		//Initializing Rest API's URL
		String APIUrl = apiBaseURI + apiMethod + "?productTypeID=87&speciesId=87";
		System.out.println(APIUrl);
		//Initializing payload or API body
		String	FilePath=Constants.inputJSONFolderpath + FileName;
		String APIBody = parseJSONFile(FilePath).toString();

		RequestSpecification requestSpec = RestAssured.with();
		requestSpec.given().contentType("application/json").body(APIBody);
		requestSpec.headers("Authorization", Constants.bearerToken);
		Response response = requestSpec.post(APIUrl);

		System.out.println("Status Code: " +response.getStatusCode());

		if (response.getStatusCode() != 200) {
			throw new RuntimeException("Failed : HTTP error code : "+ response.getStatusCode());
		}
		else {
			postResponse=response.body().asString();
		}

		return response;
	}

	public static boolean validateIfJasonArray(JSONObject JsonObj,String key) {
		boolean isJsonArray=true;
		try {
			org.json.JSONArray JsonArray = JsonObj.getJSONArray(key);

		}
		catch(Exception ex) {
			isJsonArray=false;
		}
		return isJsonArray;
	}

	/* This function will validate single key and value against simple JSON without childs/arrays*/
	public static void JSONFieldsValidatorForSimpleJSON(List<List<String>> fieldsToValidateList ){
		String keyToValidate=null;
		String valueToValidate=null;
		int failCount=0;
		for (int ilist=0; ilist<fieldsToValidateList.size();ilist++)
		{
			keyToValidate=fieldsToValidateList.get(ilist).get(0);
			valueToValidate=fieldsToValidateList.get(ilist).get(1);
			JSONObject actualJson = new JSONObject(postResponse);
			String actualValue=actualJson.getString(keyToValidate);
			if (!(valueToValidate.trim().equalsIgnoreCase(actualValue.trim()))) {

				failCount=failCount+1;
				JsonValidationReporter.addStep("Expected:-> " + keyToValidate + " : " + valueToValidate + " Actual :->" + actualValue);
			}
		}
		assertTrue(failCount==0);
	}

	/* This function will validate a given list of key and values against actual JSON*/
	public static void JSONFieldsValidator(List<List<String>> fieldsToValidateList ){
		String keyToValidate=null;
		String valueToValidate=null;
		int failCount=0;
		int count=0;
		for (int ilist=0; ilist<fieldsToValidateList.size();ilist++)
		{
			keyToValidate=fieldsToValidateList.get(ilist).get(0);
			valueToValidate=fieldsToValidateList.get(ilist).get(1);
			JSONObject actualJson = new JSONObject(postResponse);
			count=validateExpectedActualFields(actualJson,keyToValidate,valueToValidate);
			failCount=failCount+count;
			System.out.println("failcount "+failCount);
		}
		assertTrue(failCount==0);
	}
	
	public static String JSONFieldsValidation(List<List<String>> fieldsToValidateList ){
		String Status=""; 
		String keyToValidate=null;
		String valueToValidate=null;
		int failCount=0;
		int count=0;
		for (int ilist=0; ilist<fieldsToValidateList.size();ilist++)
		{
			keyToValidate=fieldsToValidateList.get(ilist).get(0);
			valueToValidate=fieldsToValidateList.get(ilist).get(1);
			JSONObject actualJson = new JSONObject(postResponse);
			count=validateExpectedActualFields(actualJson,keyToValidate,valueToValidate);
			failCount=failCount+count;
			System.out.println("failcount "+failCount);
		}
		if (failCount==0)
			Status="PASS";
		else
			Status="FAIL";
		
		return Status;
	}
	
	public static String getResponseFieldValue(JSONObject actualJsonObj, String KeyName){
		String JasonKeyValue="";;
		Iterator<String> actualKeyIterator=actualJsonObj.keys();
		org.json.JSONArray actualJsonArray;
		
		while(actualKeyIterator.hasNext()) {	
			//Geting a key from Json Object
			String actualkey=actualKeyIterator.next().toString();
			//System.out.println(actualkey);

			//Casting JSON object into Json Array
			try {
				if (validateIfJasonArray(actualJsonObj, actualkey)) {
					actualJsonArray = actualJsonObj.getJSONArray(actualkey);

					//System.out.println(actualJsonArray.length());
					//Traversing on the JASON Array
					for (int i=0; i<actualJsonArray.length(); i++) {
						JSONObject actualChildJsonObj=actualJsonArray.getJSONObject(i);

						Iterator<String> actualChildKeys=actualChildJsonObj.keys();

						while(actualChildKeys.hasNext()) {
							String actualKeyName=actualChildKeys.next().toString();
							Object actualkeyValue = actualChildJsonObj.get(actualKeyName);

							if(actualKeyName.trim().equalsIgnoreCase(KeyName.trim())){
								JasonKeyValue= actualkeyValue.toString();
								break;
							}
							if (actualChildJsonObj.get(actualKeyName) instanceof JSONObject)
								getResponseFieldValue((JSONObject)actualkeyValue, KeyName);
						}

					}
				}
				//If the key is not a Json Array
				else {

					Object actualkeyValue = actualJsonObj.get(actualkey);

					if(actualkey.trim().equalsIgnoreCase(KeyName.trim())){
						JasonKeyValue= actualkeyValue.toString();
						break;
					}
				}
			}
			catch(Exception ex) {
				System.out.println(ex.getMessage());
			}

		}
		
		return JasonKeyValue;
		
	}
	
	// HashMap<String, String> capitalCities = new HashMap<String, String>();
	
	public static int validateExpectedActualFields(JSONObject actualJsonObj,String expectedKeyName, String expectedkeyValue) {
		int failCount=0;

		Iterator<String> actualKeyIterator=actualJsonObj.keys();
		org.json.JSONArray actualJsonArray;

		while(actualKeyIterator.hasNext()) {	
			//Geting a key from Json Object
			String actualkey=actualKeyIterator.next().toString();
			//System.out.println(actualkey);

			//Casting JSON object into Json Array
			try {
				if (validateIfJasonArray(actualJsonObj, actualkey)) {
					actualJsonArray = actualJsonObj.getJSONArray(actualkey);

					//System.out.println(actualJsonArray.length());
					//Traversing on the JASON Array
					for (int i=0; i<actualJsonArray.length(); i++) {
						JSONObject actualChildJsonObj=actualJsonArray.getJSONObject(i);

						Iterator<String> actualChildKeys=actualChildJsonObj.keys();

						while(actualChildKeys.hasNext()) {
							String actualKeyName=actualChildKeys.next().toString();
							Object actualkeyValue = actualChildJsonObj.get(actualKeyName);

							if(actualKeyName.trim().equalsIgnoreCase(expectedKeyName.trim())){
								if (!(actualkeyValue.equals(expectedkeyValue)) ) {
									failCount=failCount+1;
									JsonValidationReporter.addStep("Expected:-> " + expectedKeyName + " : " + expectedkeyValue.toString() + " Actual :->" + actualKeyName + " : " + actualkeyValue.toString());
								}	
							}
							if (actualChildJsonObj.get(actualKeyName) instanceof JSONObject)
								validateExpectedActualFields((JSONObject)actualkeyValue, expectedKeyName,expectedkeyValue);
						}

					}
				}
				//If the key is not a Json Array
				else {

					Object actualkeyValue = actualJsonObj.get(actualkey);

					if(actualkey.trim().equalsIgnoreCase(expectedKeyName.trim())){
						if (!(actualkeyValue.equals(expectedkeyValue)) ) {
							failCount=failCount+1;
							JsonValidationReporter.addStep("Expected:-> " + expectedKeyName + " : " + expectedkeyValue.toString() + " Actual :->" + actualkey + " : " + actualkeyValue.toString());
						}
					}
				}
			}
			catch(Exception ex) {
				System.out.println(ex.getMessage());
			}

		}
		return failCount;

	}

	/* This function is to compare expected and actual JSONs. It will validate complete details like every key and value combinations with case sensitive
	 */
	public static void validateCompleteJsonFile(String expectedJsonFilePath) throws IOException {

		System.out.println(getApiResponse().asString());
		JSONObject actualJson = new JSONObject(getApiResponse().asString()) ;
		JSONObject expectedJson = parseJSONFile(expectedJsonFilePath);
		//JSONObject actualJson =parseJSONFile(actualJsonFilePath);

		ObjectMapper mapper = new ObjectMapper();
		assertEquals(mapper.readTree(expectedJson.toString()),mapper.readTree(actualJson.toString()));


	}
	
	/* This function is to compare expected and actual JSONs. It will validate complete details like every key and value combinations with case sensitive
	 */
	public static void validateCompleteJsonFileForPostResponse(String expectedJsonFilePath) throws IOException {

		System.out.println(postResponse);
		JSONObject actualJson = new JSONObject(postResponse) ;
		JSONObject expectedJson = parseJSONFile(expectedJsonFilePath);
		//JSONObject actualJson =parseJSONFile(actualJsonFilePath);

		ObjectMapper mapper = new ObjectMapper();
		assertEquals(mapper.readTree(expectedJson.toString()),mapper.readTree(actualJson.toString()));


	}
	
	public static void validateXMLResponse(String expectedXMLFilePath) {
		String actualData=getApiResponse().asString();
		String ExpectedXMLData=readXMLFile(expectedXMLFilePath);
		assertXMLEquals(ExpectedXMLData, actualData);
	}
	
	public static void assertXMLEquals(String expectedXML, String actualXML)  {
		try {
		XMLUnit.setIgnoreWhitespace(true);
		XMLUnit.setIgnoreAttributeOrder(true);

		DetailedDiff diff = new DetailedDiff(XMLUnit.compareXML(expectedXML, actualXML));

		List<?> allDifferences = diff.getAllDifferences();
		Assert.assertEquals("Differences found: "+ diff.toString(), 0, allDifferences.size());
		}
		catch(Exception ex) {
			System.out.println(ex.getMessage());
		}
	}

	

	public static void createXMLFile(String data, String FileName) {
		OutputStream os = null;
		try {
			os = new FileOutputStream(new File(Constants.expectedJSONFolderpath + FileName));
			os.write(data.getBytes(), 0, data.length());
		} catch (IOException e) {
			e.printStackTrace();
		}finally{
			try {
				os.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static String readXMLFile(String filePath) {
		// our XML file for this example
		String xml2String=null;
		try {
			File xmlFile = new File(filePath);


			Reader fileReader = new FileReader(xmlFile);
			BufferedReader bufReader = new BufferedReader(fileReader);

			StringBuilder sb = new StringBuilder();
			String line = bufReader.readLine();
			while( line != null){
				sb.append(line).append("\n");
				line = bufReader.readLine();
			}
			xml2String = sb.toString();
		}
		catch(Exception ex) {
			System.out.println(ex.getMessage());
		}
		
		return xml2String;
	}
	
	public static Response getRequestWithParametrInURI(String AuthorizeToken) throws JSONException, IOException {

		//Initializing Rest API's URL
		String APIUrl = apiBaseURI + apiMethod + paraPropNameValue;
		System.out.println(APIUrl);
			

		RequestSpecification requestSpec = RestAssured.with();
		requestSpec.given().contentType("application/json");
		requestSpec.headers("Authorization", AuthorizeToken);
		Response response = requestSpec.get(APIUrl);

		System.out.println("Status Code: " +response.getStatusCode());

		if (response.getStatusCode() != 200) {
			throw new RuntimeException("Failed : HTTP error code : "+ response.getStatusCode());
		}
		else {
			postResponse=response.body().asString();
		}

		return response;
	}

}
