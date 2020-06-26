package appModules;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.json.JSONObject;
import com.jayway.restassured.response.Response;
import resourceManagers.Constants;
import utils.ApiUtils;

public class AddToCartAPIMethods {
	
	private int pgpNameCol=0;
	private int CatNumCol=1;
	private int StatusCodeCol=2;
	private int MessageCol=3;
	private int materialNumberCol=4;
	private int IsValidCol=5;
	private int PricingKeyCol=6;
	private int StatusColumnIndex=7;
	private Response apiResponse;
	
	
	public String validateGetAddToCartAPI(String baseUri,String catLogNumber,String ExpectedStatus) throws IOException {
		String Status="";
		ApiUtils.setApiURI(baseUri);	
		 ApiUtils.setAPIMethod("getAddToCart");
		 ApiUtils.setAPIParameter(getParameterList(catLogNumber));
		 Response res= ApiUtils.getRequestWithParametrInURI(Constants.BasicAuthAddToCart);
		 setApiResponse(res);
		 System.out.println(res.body().asString());
		 Status=ApiUtils.JSONFieldsValidation(getAddToCartAPIExpectedResponse(ExpectedStatus));
		 
		 return Status;
	}
	
	public HashMap<String, String> getAddToCartResponseFields(Response res) {
		HashMap<String, String> JasonKeyValueMap=new HashMap<String, String>();
		JSONObject actualJson = new JSONObject(res.body().asString());
		
		JasonKeyValueMap.put("Message", ApiUtils.getResponseFieldValue(actualJson, "Message"));
		JasonKeyValueMap.put("materialNumber", ApiUtils.getResponseFieldValue(actualJson, "materialNumber"));
		JasonKeyValueMap.put("StatusCode", ApiUtils.getResponseFieldValue(actualJson, "Status"));
		JasonKeyValueMap.put("IsValid", ApiUtils.getResponseFieldValue(actualJson, "IsValid"));
		JasonKeyValueMap.put("PricingKey", ApiUtils.getResponseFieldValue(actualJson, "PricingKey"));
			
		
		return JasonKeyValueMap;
	}
	
	public static List<List<String>> getParameterList(String catLogNumber){
		List<List<String>> parameterList= new ArrayList<List<String>>();
		 List<String> CatLogNameValue=new ArrayList<String>();
		 List<String> CountryCodeNameValue=new ArrayList<String>();
		 List<String> UserIDNameValue=new ArrayList<String>();
		 
		 CatLogNameValue.add("CatalogNumber");
		 CatLogNameValue.add(catLogNumber); //"QT000000427A"
		
		 CountryCodeNameValue.add("CountryCode");
		 CountryCodeNameValue.add("de");
		
		 UserIDNameValue.add("UserId");
		 UserIDNameValue.add("1234");
		 
		 parameterList.add(CatLogNameValue);
		 parameterList.add(CountryCodeNameValue);
		 parameterList.add(UserIDNameValue);
		 
		 return parameterList;
	}
	
	public static List<List<String>> getAddToCartAPIExpectedResponse(String Status){
		List<List<String>> JsonFieldList= new ArrayList<List<String>>();
		 List<String> StatusNameValue=new ArrayList<String>();
		 StatusNameValue.add("Status");
		 StatusNameValue.add(Status);
		 
		 JsonFieldList.add(StatusNameValue);
		 
		 return JsonFieldList;
	}

	//Getter for accessing private variables
	public int getPricingKeyCol() {
		return PricingKeyCol;
	}

	public int getPgpNameCol() {
		return pgpNameCol;
	}


	public int getStatusCodeCol() {
		return StatusCodeCol;
	}

	

	public int getCatNumCol() {
		return CatNumCol;
	}

	
	public int getMaterialNumberCol() {
		return materialNumberCol;
	}

	

	public int getMessageCol() {
		return MessageCol;
	}

	

	public int getIsValidCol() {
		return IsValidCol;
	}

	

	public int getStatusColumnIndex() {
		return StatusColumnIndex;
	}

	public Response getApiResponse() {
		return apiResponse;
	}

	public void setApiResponse(Response apiResponse) {
		this.apiResponse = apiResponse;
	}

	

	
	
}
