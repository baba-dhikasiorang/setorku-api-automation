package com.setorku.steps

import cucumber.api.java.en.Given
import cucumber.api.java.en.When
import cucumber.api.java.en.Then
import com.kms.katalon.core.testobject.RequestObject
import com.kms.katalon.core.testobject.ResponseObject
import com.kms.katalon.core.testobject.RestRequestObjectBuilder
import com.kms.katalon.core.testobject.TestObjectProperty
import com.kms.katalon.core.testobject.ConditionType
import com.kms.katalon.core.webservice.keyword.WSBuiltInKeywords as WS
import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import com.setorku.utils.SignatureUtil

public class SetorkuApiSteps {

    private String baseUrl = "https://ts.setorku.com/Setorku/webresources"
    private String secretKey = "IDS!2345"
    private ResponseObject response
    private String generatedKodeBayar

    @Given("endpoint API {string} siap menerima request")
    public void prepareEndpoint(String endpoint) {
        assert endpoint != null && !endpoint.isEmpty()
    }

    @Given("kode bayar {string} terdaftar di DB Setorku dan berstatus belum dibayar")
    public void prepareExistingBill(String kodeBayarOption) {
        if (!kodeBayarOption.equalsIgnoreCase("EXISTING")) {
            this.generatedKodeBayar = kodeBayarOption
        }
    }

    @Given("payload request inquiry disusun dengan partnerid {string}, productid {string}, dan signature SHA1 valid")
    public void prepareInquiryPayload(String partnerid, String productid) {
        assert partnerid != null && productid != null
    }

    @Given("proses inquiry untuk kode bayar {string} telah berhasil dilakukan")
    public void preparePaymentInquiryState(String kodeBayarOption) {
        assert this.generatedKodeBayar != null || !kodeBayarOption.isEmpty()
    }

    @Given("payload pembayaran dikirimkan dengan nominal yang sesuai dan signature valid")
    public void preparePaymentPayload() {
        assert true
    }

    // ==========================================
    // TAHAP 1: SetorDTH (Generate Setor Code)
    // ==========================================
    @When("client mengirimkan request POST ke {string} dengan signature {string}")
    public void sendPostSetorDTH(String endpoint, String signatureOption) {
        String trxid = "A" + System.currentTimeMillis()
        String trxdate = "2026-10-08 12:00:00"
        String productid = "501"
        String merchanttransactionid = "2342202727"
        String usercode = "220223041"
        String username = "Umar Haryono"
        String totalbill = "1"
        String totalamount = "869000"
        String trackingref = "A0922004944"
        String notifUrl = "https://example.com/api/v1/payment/notification"
        String companyid = "1"
        String customerid = "2342200019"
        String periode = "7"

        String rawSig = trxid + trxdate + productid + merchanttransactionid + usercode + username + totalbill + totalamount + trackingref + notifUrl + companyid + customerid + totalamount + periode + secretKey
        String calculatedSignature = signatureOption.equalsIgnoreCase("VALID") ? SignatureUtil.generateSHA1(rawSig) : "INVALID_SIGNATURE_HASH_12345"

        Map payloadMap = [
            trxid: trxid,
            trxdate: trxdate,
            productid: productid,
            merchanttransactionid: merchanttransactionid,
            usercode: usercode,
            username: username,
            totalbill: totalbill,
            totalamount: totalamount,
            trackingref: trackingref,
            signature: calculatedSignature,
            detail: [[
                customerid: customerid,
                totalamount: totalamount,
                companyid: companyid,
                periode: periode
            ]],
            notif_url: notifUrl
        ]

        response = executePostRequest(endpoint, JsonOutput.toJson(payloadMap))
    }

    // ==========================================
    // TAHAP 2: SetorInq (Merchant Inquiry)
    // ==========================================
    @When("merchant mengirimkan request POST inquiry ke {string} untuk kode bayar {string}")
    public void sendPostSetorInq(String endpoint, String kodeBayarInput) {
        String targetKodeBayar = kodeBayarInput.equalsIgnoreCase("EXISTING") ? this.generatedKodeBayar : kodeBayarInput
        
        String trxid = "AS" + System.currentTimeMillis()
        String trxdate = "2026-10-08 12:05:00"
        String trackingref = "248470266"
        String productid = "STR"
        String partnerid = "DMY"

        String rawSig = trxid + trxdate + trackingref + productid + partnerid + targetKodeBayar
        String signature = SignatureUtil.generateSHA1(rawSig)

        Map payloadMap = [
            trxid: trxid,
            trxdate: trxdate,
            trackingref: trackingref,
            productid: productid,
            partnerid: partnerid,
            kodebayar: targetKodeBayar,
            signature: signature
        ]

        response = executePostRequest(endpoint, JsonOutput.toJson(payloadMap))
    }

    // ==========================================
    // TAHAP 3: SetorPay (Merchant Payment)
    // ==========================================
    @When("merchant mengirimkan request POST payment ke {string} dengan nominal {string}")
    public void sendPostSetorPay(String endpoint, String amount) {
        String trxid = "AS" + System.currentTimeMillis()
        String trxdate = "2026-10-08 12:10:00"
        String trackingref = "648690726"
        String productid = "STR"
        String partnerid = "DMY"

        String rawSig = trxid + trxdate + trackingref + productid + partnerid + this.generatedKodeBayar + amount
        String signature = SignatureUtil.generateSHA1(rawSig)

        Map payloadMap = [
            trxid: trxid,
            trxdate: trxdate,
            trackingref: trackingref,
            productid: productid,
            partnerid: partnerid,
            kodebayar: this.generatedKodeBayar,
            amount: amount,
            signature: signature
        ]

        response = executePostRequest(endpoint, JsonOutput.toJson(payloadMap))
    }

    // ==========================================
    // ASSERTIONS & VERIFICATION
    // ==========================================
    @Then("response status code bernilai {int}")
    public void verifyStatusCode(int expectedCode) {
        WS.verifyResponseStatusCode(response, expectedCode)
    }

    @Then("response body mengembalikan rc {string} dan rcdesc {string}")
    public void verifyResponseRC(String expectedRc, String expectedRcDesc) {
        def json = new JsonSlurper().parseText(response.getResponseBodyContent())
        assert json.rc == expectedRc
        assert json.rcdesc == expectedRcDesc

        if (json.ls_kodebayar != null && json.ls_kodebayar.size() > 0) {
            this.generatedKodeBayar = json.ls_kodebayar[0].kodebayar
        }
    }

    // Helper Runner Method
    private ResponseObject executePostRequest(String endpoint, String jsonPayload) {
        TestObjectProperty headerContentType = new TestObjectProperty("Content-Type", ConditionType.EQUALS, "application/json")
        
        RequestObject request = new RestRequestObjectBuilder()
            .withRestUrl(baseUrl + endpoint)
            .withHttpHeaders([headerContentType])
            .withRestRequestMethod("POST")
            .withTextBodyContent(jsonPayload)
            .build()

        return WS.sendRequest(request)
    }
}
