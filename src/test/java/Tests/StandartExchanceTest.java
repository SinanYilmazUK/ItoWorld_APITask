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

public class StandartExchanceTest {

    private static final Logger log = LogManager.getLogger(StandartExchanceTest.class.getName());

    @BeforeClass
    public void beforeClass() {
        baseURI = ConfigurationReader.get("exchanceRateBaseURI");
    }

    @Test
    public void standartExchangeRate_PositiveTest() {

        Response response = RestAssured.given().contentType(ContentType.JSON)
                .and().pathParam("api-key", ConfigurationReader.get("api-key"))
                .and().pathParam("currency", ConfigurationReader.get("currency"))
                .when().get("/{api-key}/latest/{currency}");

        response.prettyPrint();

        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertEquals(response.contentType(), "application/json");

        // header
        Assert.assertEquals(response.header("Content-Type"), "application/json");

        // GPATH syntax

        String result = response.path("result");     // to read response body we used path method
        log.info("result = " + result);
        Assert.assertEquals(result, "success");

        // create a jsonPath object from response object

        JsonPath jsonPath = response.jsonPath();

        Assert.assertEquals(jsonPath.getString("documentation"), "https://www.exchangerate-api.com/docs");
        Assert.assertEquals(jsonPath.getString("terms_of_use"), "https://www.exchangerate-api.com/terms");
        Assert.assertEquals(jsonPath.getString("base_code"), ConfigurationReader.get("currency"));
        Assert.assertEquals(jsonPath.getDouble("conversion_rates." + ConfigurationReader.get("currency")), 1);

        int lastDate = Integer.parseInt(jsonPath.getString("time_last_update_unix"));
        int nextDate = Integer.parseInt(jsonPath.getString("time_next_update_unix"));

        // Check if the difference is exactly one day

        Assert.assertEquals(nextDate - lastDate, 86400, "There is NOT exactly one day between the two dates.");

    }

    @Test
    public void standartExchangeRate_NegativeTest(){

        /***
         * Provided incorrect API KEY values
         *
         * {
         *     "result": "error",
         *     "documentation": "https://www.exchangerate-api.com/docs",
         *     "terms-of-use": "https://www.exchangerate-api.com/terms",
         *     "error-type": "inactive-account",
         *     "extra-info": "Please contact support."
         * }
         */
        Response response = RestAssured.given().contentType(ContentType.JSON)
                .and().pathParam("api-key", ConfigurationReader.get("incorrect-api-key"))
                .when().get("/{api-key}/latest/USD");

        response.prettyPrint();

        Assert.assertEquals(response.statusCode(), 403);

        JsonPath jsonPath = response.jsonPath();
        String result = jsonPath.getString("result");
        log.info("result = " + result);
        Assert.assertEquals(result, "error");
        Assert.assertEquals(jsonPath.getString("extra-info"), "Please contact support.");
        Assert.assertEquals(jsonPath.getString("error-type"), "inactive-account", "The error type is NOT correct.");

    }

    /***
     * Provided incorrect currency
     *
     * {
     *     "result": "error",
     *     "documentation": "https://www.exchangerate-api.com/docs",
     *     "terms-of-use": "https://www.exchangerate-api.com/terms",
     *     "error-type": "unsupported-code"
     * }
     */

    @Test
    public void standartExchangeRate_NegativeTest2(){
        Response response = RestAssured.given().contentType(ContentType.JSON)
                .and().pathParam("api-key", ConfigurationReader.get("api-key"))
                .when().get("/{api-key}/latest/US");

        response.prettyPrint();

        Assert.assertEquals(response.statusCode(), 404);

        JsonPath jsonPath = response.jsonPath();
        String result = jsonPath.getString("result");
        log.info("result = " + result);
        Assert.assertEquals(result, "error");
        Assert.assertEquals(jsonPath.getString("error-type"), "unsupported-code", "The error type is NOT correct.");

    }
}
