package Tests;

import Utilities.ConfigurationReader;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static io.restassured.RestAssured.baseURI;

public class PairExchangeTest {

    private static final Logger log = LogManager.getLogger(StandartExchanceTest.class.getName());

    @BeforeClass
    public void beforeClass() {
        baseURI = ConfigurationReader.get("exchanceRateBaseURI");
    }

    @Test
    public void pairExchangeRate_PositiveTest() {

        Response response = RestAssured.given().contentType(ContentType.JSON)
                .and().pathParam("api-key", ConfigurationReader.get("api-key"))
                .and().pathParam("base_code", ConfigurationReader.get("base_code"))
                .and().pathParam("target_code", ConfigurationReader.get("target_code"))

                .when().get("{api-key}/pair/{base_code}/{target_code}");

        response.prettyPrint();

        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertEquals(response.contentType(), "application/json");

        Assert.assertEquals(response.header("Content-Type"), "application/json");

        String result = response.path("result");
        log.info("result = " + result);
        Assert.assertEquals(result, "success");

        JsonPath jsonPath = response.jsonPath();

        Assert.assertEquals(jsonPath.getString("documentation"), "https://www.exchangerate-api.com/docs");
        Assert.assertEquals(jsonPath.getString("terms_of_use"), "https://www.exchangerate-api.com/terms");
        Assert.assertEquals(jsonPath.getString("base_code"), ConfigurationReader.get("base_code"));
        Assert.assertEquals(jsonPath.getString("target_code"), ConfigurationReader.get("target_code"));

        int lastDate = Integer.parseInt(jsonPath.getString("time_last_update_unix"));
        int nextDate = Integer.parseInt(jsonPath.getString("time_next_update_unix"));

        Assert.assertEquals(nextDate - lastDate, 86400, "There is NOT exactly one day between the two dates.");
    }

    @Test
    public void PairExchangeRate_NegativeTest(){

        /***
         * Provided incorrect API KEY values
         */
        Response response = RestAssured.given().contentType(ContentType.JSON)
                .and().pathParam("api-key", ConfigurationReader.get("incorrect-api-key"))
                .when().get("/{api-key}/pair/EUR/GBP");

        response.prettyPrint();

        Assert.assertEquals(response.statusCode(), 403);

        JsonPath jsonPath = response.jsonPath();
        String result = jsonPath.getString("result");
        log.info("result = " + result);
        Assert.assertEquals(result, "error");
        Assert.assertEquals(jsonPath.getString("extra-info"), "Please contact support.");
        String errorType = jsonPath.getString("error-type");
        if (!errorType.equals("inactive-account") && !errorType.equals("invalid-key")) {
            Assert.fail("The error type is not as expected (inactive-account or invalid-key)");
        }
    }
}
