package utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Color;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.mockito.internal.stubbing.answers.ThrowsException;
import org.testng.Assert;

import appModules.AddToCartAPIMethods;
import resourceManagers.ConfigFileReader;

public class ExcelFileReaderPage {


	//	SearchPage searchPage = PageFactory.initElements(driver, SearchPage.class);
	//	ConfigFileReader configReader  = PageFactory.initElements(driver, ConfigFileReader.class);
	AddToCartAPIMethods addToCartMethod= new AddToCartAPIMethods();

	String baseUrl;
	String fileName;
	String sheetName;
	File file;
	private XSSFWorkbook  workbook;
	private XSSFSheet sheet;
	String env;




	public void setFileName(String excelFileName, String SheetName) {
		if (System.getProperty("os.name").toLowerCase().contains("linux")) {
			fileName = System.getProperty("user.dir")+"//TestData//" + excelFileName + ".xlsx";
		}
		else if (System.getProperty("os.name").toLowerCase().contains("windows")) {
			fileName = System.getProperty("user.dir")+"\\TestData\\" + excelFileName + ".xlsx";
		}
		sheetName=SheetName;
	}

	public String getFileName() {
		return fileName;
	}

	public String getSheetName() {
		return sheetName;
	}


	public static String getCatalogNumberFromSheet(XSSFRow row, int ColumnIndex) throws Exception{
		String CatalogNumber;
		try {
			CatalogNumber=row.getCell(ColumnIndex).getStringCellValue();
		}
		catch(Exception ex) {
			CatalogNumber=null;
		}
		return CatalogNumber;
	}


	public XSSFSheet openWorkbook() throws Exception {
		XSSFSheet sheet;
		File file;
		FileInputStream  fileIS = null;

		try {
			file = new File(getFileName());
			fileIS = new FileInputStream(file);
			workbook = new XSSFWorkbook(fileIS);
			sheet = workbook.getSheet(getSheetName());
		}
		catch(Exception ex) {
			sheet=null;
			throw new Exception(ex.getMessage() +" Might be due to Invalid Excel File or Sheet Name");
		}
		finally {
			fileIS.close();
		}

		return sheet;
	}

	public void closeWorkbook() throws IOException {
		workbook.close();
	}

	public void eraseCellData(XSSFSheet XlSheet) throws Exception {
		//		openWorkbook();
		int rowCount = XlSheet.getLastRowNum();
		XSSFRow rows;

		for (int irow=1;irow<=rowCount;irow++) {
			rows=XlSheet.getRow(irow);

			XSSFCell cellStatusCode = rows.getCell(addToCartMethod.getStatusCodeCol());
			if (cellStatusCode!=null) 
				rows.removeCell(cellStatusCode);

			//For Message
			XSSFCell cellMessage = rows.getCell(addToCartMethod.getMessageCol());
			if (cellStatusCode!=null) 
				rows.removeCell(cellMessage);


			//For materialNumber
			XSSFCell cellMaterialNumber = rows.getCell(addToCartMethod.getMaterialNumberCol());
			if (cellStatusCode!=null) 
				rows.removeCell(cellMaterialNumber);

			//For IsValid
			XSSFCell cellIsValid = rows.getCell(addToCartMethod.getIsValidCol());
			if (cellStatusCode!=null) 
				rows.removeCell(cellIsValid);

			//For PricingKey
			XSSFCell cellPricingKey = rows.getCell(addToCartMethod.getPricingKeyCol());
			if (cellStatusCode!=null) 
				rows.removeCell(cellPricingKey);


			//For Status 
			XSSFCell cellStatus = rows.getCell(addToCartMethod.getStatusColumnIndex());
			if (cellStatusCode!=null) 
				rows.removeCell(cellStatus);

			FileOutputStream fileOut = new FileOutputStream(getFileName());
			workbook.write(fileOut);
			fileOut.close();
		}
	}






	public void validateGetToCartAPIValidInvalidCatalog(String baseUri,boolean blnValid) throws Exception {

		//Initializing variables 
		String pGPName="";
		String catalogNumber="";
		String Status="";
		String ExpectedStatus="";
		HashMap<String, String> JasonKeyValueMap=new HashMap<String, String>();
		XSSFRow row;
		
		//Open Workbook
		XSSFSheet sheet=openWorkbook();
		
		//Erase Old Data
		eraseCellData(sheet);

		int rowCount = sheet.getLastRowNum();
		System.out.println("Row count: " + rowCount);


		for(int i = 1; i<=rowCount; i++) {
			Status="";
			row = sheet.getRow(i);
			pGPName=row.getCell(0).getStringCellValue();
			catalogNumber=getCatalogNumberFromSheet(row,addToCartMethod.getCatNumCol());

			if (blnValid) {
				ExpectedStatus="0";
			}
			else {
				ExpectedStatus="1";
			}
			
			if(catalogNumber!=null && !catalogNumber.isEmpty() ) {
				try {
					//Hit the API and validate response
					Status=addToCartMethod.validateGetAddToCartAPI(baseUri, catalogNumber, ExpectedStatus);

					//Return Reponse in a Hash Map
					JasonKeyValueMap=addToCartMethod.getAddToCartResponseFields(addToCartMethod.getApiResponse());
					JasonKeyValueMap.put("Status", Status);
					setScenarioStatus(sheet,i,JasonKeyValueMap);

				}catch (Exception e) {
					JasonKeyValueMap.put("Status", "FAIL");
					JasonKeyValueMap.put("Message", e.getMessage());
					setScenarioStatus(sheet,i,JasonKeyValueMap);

				}
				System.out.println("**********************");
				System.out.println(JasonKeyValueMap);
				System.out.println("**********************");
			}
		}
	}



	public void setScenarioStatus(XSSFSheet sheet,int rowNumber,HashMap<String, String> JasonKeyValueMap) throws IOException{

		XSSFRow rows = sheet.getRow(rowNumber);
		
		//For Status code
		XSSFCell cellStatusCode = rows.getCell(addToCartMethod.getStatusCodeCol());
		if (cellStatusCode==null) 
			cellStatusCode=rows.createCell(addToCartMethod.getStatusCodeCol());
		cellStatusCode.setCellValue(JasonKeyValueMap.get("StatusCode"));

		//For Message
		XSSFCell cellMessage = rows.getCell(addToCartMethod.getMessageCol());
		if (cellMessage==null) 
			cellMessage=rows.createCell(addToCartMethod.getMessageCol());
		cellMessage.setCellValue(JasonKeyValueMap.get("Message"));


		//For materialNumber
		XSSFCell cellMaterialNumber = rows.getCell(addToCartMethod.getMaterialNumberCol());
		if (cellMaterialNumber==null) 
			cellMaterialNumber=rows.createCell(addToCartMethod.getMaterialNumberCol()); 
		cellMaterialNumber.setCellValue(JasonKeyValueMap.get("materialNumber"));

		//For IsValid
		XSSFCell cellIsValid = rows.getCell(addToCartMethod.getIsValidCol());
		if (cellIsValid==null) 
			cellIsValid=rows.createCell(addToCartMethod.getIsValidCol());
		cellIsValid.setCellValue(JasonKeyValueMap.get("IsValid"));

		//For PricingKey
		XSSFCell cellPricingKey = rows.getCell(addToCartMethod.getPricingKeyCol());
		if (cellPricingKey==null) 
			cellPricingKey=rows.createCell(addToCartMethod.getPricingKeyCol());
		cellPricingKey.setCellValue(JasonKeyValueMap.get("PricingKey"));


		//For Status 
		String status=JasonKeyValueMap.get("Status");
		XSSFCell cellStatus = rows.getCell(addToCartMethod.getStatusColumnIndex());
		if (cellStatus==null) 
			cellStatus=rows.createCell(addToCartMethod.getStatusColumnIndex());
		cellStatus.setCellValue(status);

		 XSSFCellStyle style = new XSSFCellStyle(workbook.getStylesSource());
		if(status.contentEquals("PASS")){	
			
			 style.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
			 style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
			 cellStatus.setCellStyle(style);

		}else if(status.contentEquals("FAIL")) {
			
			 style.setFillForegroundColor(IndexedColors.RED.getIndex());
			 style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
			 cellStatus.setCellStyle(style);
		}
		else {
			
			cellStatus.setCellValue("NA");
			 style.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
			 style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
			 cellStatus.setCellStyle(style);
		}

		FileOutputStream output_file =new FileOutputStream(fileName); 

		workbook.write(output_file);
		output_file.close();
	}


}



