package com.box.sdk;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.matching.ContainsPattern;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.github.tomakehurst.wiremock.matching.MultipartValuePatternBuilder;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * {@link BoxUser} related tests.
 */

public class BoxUserTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort());
    private final BoxAPIConnection api = TestConfig.getAPIConnection();

    @Before
    public void setUpBaseUrl() {
        api.setMaxRetryAttempts(1);
        api.setBaseURL(format("http://localhost:%d", wireMockRule.port()));
    }

    @Test
    public void testGetAvatar() throws IOException {
        final String expectedURL = "/2.0/users/12345/avatar";
        File file = new File("src/test/Fixtures/BoxUser/small_avatar.png");
        byte[] fileByteArray = Files.readAllBytes(file.toPath());

        wireMockRule.stubFor(WireMock.get(WireMock.urlPathEqualTo(expectedURL))
            .willReturn(WireMock.aResponse()
                .withHeader("Content-Type", "image/png")
                .withBody(fileByteArray)));

        BoxUser user = new BoxUser(this.api, "12345");
        InputStream avatarStream = user.getAvatar();

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[10000];
        try {
            int n = avatarStream.read(buffer);
            while (n != -1) {
                output.write(buffer, 0, n);
                n = avatarStream.read(buffer);
            }
        } catch (IOException e) {
            throw new BoxAPIException("Couldn't connect to the Box API due to a network error.", e);
        }

        Assert.assertArrayEquals(fileByteArray, output.toByteArray());
    }

    @Test
    public void testGetCurrentUserInfoSucceeds() throws IOException {
        final String userURL = "/2.0/users/me";
        final String userInfoURL = "/2.0/users/12345";
        final String userName = "Test User";
        final String userLogin = "test@user.com";
        final String userphoneNumber = "1111111111";

        String result = TestConfig.getFixture("BoxUser/GetCurrentUserInfo200");

        wireMockRule.stubFor(WireMock.get(WireMock.urlPathEqualTo(userURL))
            .willReturn(WireMock.aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(result)));

        wireMockRule.stubFor(WireMock.get(WireMock.urlPathEqualTo(userInfoURL))
            .willReturn(WireMock.aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(result)));

        BoxUser user = BoxUser.getCurrentUser(this.api);
        BoxUser.Info info = user.getInfo();

        Assert.assertEquals(userName, info.getName());
        Assert.assertEquals(userLogin, info.getLogin());
        Assert.assertEquals(userphoneNumber, info.getPhone());
    }

    @Test
    public void testGetUserInfoByIDSucceeds() throws IOException {
        final String userID = "12345";
        final String userURL = "/2.0/users/" + userID;
        final String userName = "Test User";
        final String userLogin = "test@user.com";
        final String userPhoneNumber = "1111111111";

        String result = TestConfig.getFixture("BoxUser/GetCurrentUserInfo200");

        wireMockRule.stubFor(WireMock.get(WireMock.urlPathEqualTo(userURL))
            .willReturn(WireMock.aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(result)));

        BoxUser user = new BoxUser(this.api, userID);
        BoxUser.Info userInfo = user.getInfo();

        Assert.assertEquals(userID, userInfo.getID());
        Assert.assertEquals(userName, userInfo.getName());
        Assert.assertEquals(userLogin, userInfo.getLogin());
        Assert.assertEquals(userPhoneNumber, userInfo.getPhone());
    }

    @Test
    public void testCreateAppUserSucceeds() throws IOException {
        final String userURL = "/2.0/users";
        final String userID = "12345";
        final String userName = "Java SDK App User";
        final String userLogin = "testuser@boxdevedition.com";

        String result = TestConfig.getFixture("BoxUser/CreateAppUser201");

        wireMockRule.stubFor(WireMock.post(WireMock.urlPathEqualTo(userURL))
            .willReturn(WireMock.aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(result)));

        BoxUser.Info createdUserInfo = BoxUser.createAppUser(this.api, userName);

        Assert.assertEquals(userID, createdUserInfo.getID());
        Assert.assertEquals(userName, createdUserInfo.getName());
        Assert.assertEquals(userLogin, createdUserInfo.getLogin());
    }

    @Test
    public void testCreateManagedUserSucceeds() throws IOException {
        final String userURL = "/2.0/users";
        final String userID = "12345";
        final String userName = "Test Managed User";
        final String userLogin = "test@user.com";
        final String userTimeZone = "America/Los_Angeles";

        String result = TestConfig.getFixture("BoxUser/CreateManagedUser201");

        wireMockRule.stubFor(WireMock.post(WireMock.urlPathEqualTo(userURL))
            .willReturn(WireMock.aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(result)));

        BoxUser.Info createdUserInfo = BoxUser.createEnterpriseUser(this.api, userLogin, userName);

        Assert.assertEquals(userID, createdUserInfo.getID());
        Assert.assertEquals(userName, createdUserInfo.getName());
        Assert.assertEquals(userLogin, createdUserInfo.getLogin());
        Assert.assertEquals(userTimeZone, createdUserInfo.getTimezone());
    }

    @Test
    public void testUpdateUserInfoSucceedsAndSendsCorrectJson() throws IOException {
        final String userID = "12345";
        final String userURL = "/2.0/users/" + userID;
        final String userName = "New User Name";
        final String userLogin = "new@test.com";
        final String userJob = "Example Job";
        final String userPhone = "650-123-4567";

        JsonObject userObject = new JsonObject()
            .add("name", userName)
            .add("login", userLogin)
            .add("job_title", userJob)
            .add("phone", userPhone);

        String result = TestConfig.getFixture("BoxUser/UpdateUserInfo200");

        wireMockRule.stubFor(WireMock.put(WireMock.urlPathEqualTo(userURL))
            .withRequestBody(WireMock.equalToJson(userObject.toString()))
            .willReturn(WireMock.aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(result)));

        BoxUser user = new BoxUser(this.api, userID);
        BoxUser.Info info = user.new Info();
        info.setName(userName);
        info.setLogin(userLogin);
        info.setJobTitle(userJob);
        info.setPhone(userPhone);
        user.updateInfo(info);

        Assert.assertEquals(userID, info.getID());
        Assert.assertEquals(userName, info.getName());
        Assert.assertEquals(userLogin, info.getLogin());
        Assert.assertEquals(userJob, info.getJobTitle());
        Assert.assertEquals(userPhone, info.getPhone());
    }

    @Test
    public void testDeleteUserSucceeds() {
        final String userID = "12345";
        final String userURL = "/2.0/users/" + userID;

        wireMockRule.stubFor(WireMock.delete(WireMock.urlPathEqualTo(userURL))
            .willReturn(WireMock.aResponse()
                .withHeader("Content-Type", "application/json")
                .withStatus(204)));

        BoxUser user = new BoxUser(this.api, userID);
        user.delete(false, false);
    }

    @Test
    public void testCreateEmailAliasSucceeds() throws IOException {
        final String userID = "12345";
        final String emailAliasURL = "/2.0/users/" + userID + "/email_aliases";
        final String emailAlias = "test@user.com";
        JsonObject emailAliasObject = new JsonObject()
            .add("email", emailAlias);

        String result = TestConfig.getFixture("BoxUser/CreateEmailAlias201");

        wireMockRule.stubFor(WireMock.post(WireMock.urlPathEqualTo(emailAliasURL))
            .withRequestBody(WireMock.equalToJson(emailAliasObject.toString()))
            .willReturn(WireMock.aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(result)));

        BoxUser user = new BoxUser(this.api, userID);
        EmailAlias alias = user.addEmailAlias(emailAlias);

        Assert.assertEquals(userID, alias.getID());
        Assert.assertTrue(alias.getIsConfirmed());
        Assert.assertEquals(emailAlias, alias.getEmail());
    }

    @Test
    public void testGetEmailAliasSucceeds() throws IOException {
        final String userID = "12345";
        final String userEmail = "test@user.com";
        final String emailAliasURL = "/2.0/users/" + userID + "/email_aliases";

        String result = TestConfig.getFixture("BoxUser/GetUserEmailAlias200");

        wireMockRule.stubFor(WireMock.get(WireMock.urlPathEqualTo(emailAliasURL))
            .willReturn(WireMock.aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(result)));

        BoxUser user = new BoxUser(this.api, userID);
        Collection<EmailAlias> emailAliases = user.getEmailAliases();

        Assert.assertEquals(userID, emailAliases.iterator().next().getID());
        Assert.assertEquals(userEmail, emailAliases.iterator().next().getEmail());
    }

    @Test
    public void testDeleteEmailAliasSucceeds() {
        final String userID = "12345";
        final String aliasID = "12345";
        final String deleteAliasURL = "/2.0/users/" + userID + "/email_aliases/" + aliasID;

        wireMockRule.stubFor(WireMock.delete(WireMock.urlPathEqualTo(deleteAliasURL))
            .willReturn(WireMock.aResponse()
                .withHeader("Content-Type", "application/json")
                .withStatus(204)));

        BoxUser user = new BoxUser(this.api, userID);
        user.deleteEmailAlias(aliasID);
    }

    @Test
    public void testGetAllEnterpriseUsersSucceeds() throws IOException {
        final String getAllUsersURL = "/2.0/users";
        final String firstUserID = "12345";
        final String firstUserName = "Test User";
        final String firstUserLogin = "test@user.com";
        final String secondUserID = "43242";
        final String secondUserName = "Example User";
        final String secondUserLogin = "example@user.com";

        String result = TestConfig.getFixture("BoxUser/GetAllEnterpriseUsers200");

        wireMockRule.stubFor(WireMock.get(WireMock.urlPathEqualTo(getAllUsersURL))
            .willReturn(WireMock.aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(result)));

        Iterator<BoxUser.Info> users = BoxUser.getAllEnterpriseUsers(this.api).iterator();
        BoxUser.Info firstUser = users.next();

        Assert.assertEquals(firstUserID, firstUser.getID());
        Assert.assertEquals(firstUserName, firstUser.getName());
        Assert.assertEquals(firstUserLogin, firstUser.getLogin());

        BoxUser.Info secondUser = users.next();

        Assert.assertEquals(secondUserID, secondUser.getID());
        Assert.assertEquals(secondUserName, secondUser.getName());
        Assert.assertEquals(secondUserLogin, secondUser.getLogin());
    }

    @Test
    public void testGetAllEnterpriseUsersMarkerPaginationSucceeds() throws IOException {
        final String getAllUsersURL = "/2.0/users";
        final String firstUserID = "12345";
        final String firstUserName = "Test User";
        final String firstUserLogin = "test@user.com";
        final String secondUserID = "43242";
        final String secondUserName = "Example User";
        final String secondUserLogin = "example@user.com";

        String result = TestConfig.getFixture("BoxUser/GetAllEnterpriseUsersMarkerPagination200");

        wireMockRule.stubFor(WireMock.get(WireMock.urlPathEqualTo(getAllUsersURL))
            .withQueryParam("usemarker", WireMock.equalTo("true"))
            .withQueryParam("limit", WireMock.equalTo("100"))
            .willReturn(WireMock.aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(result)));

        Iterator<BoxUser.Info> users = BoxUser.getAllEnterpriseUsers(this.api, true, null).iterator();
        BoxUser.Info firstUser = users.next();

        Assert.assertEquals(firstUserID, firstUser.getID());
        Assert.assertEquals(firstUserName, firstUser.getName());
        Assert.assertEquals(firstUserLogin, firstUser.getLogin());

        BoxUser.Info secondUser = users.next();

        Assert.assertEquals(secondUserID, secondUser.getID());
        Assert.assertEquals(secondUserName, secondUser.getName());
        Assert.assertEquals(secondUserLogin, secondUser.getLogin());
    }

    @Test
    public void testTransferContent() throws IOException {
        final String sourceUserID = "1111";
        final String destinationUserID = "5678";
        final String createdByLogin = "test@user.com";
        final String transferredFolderName = "Example Test Folder";
        final String transferContentURL = "/2.0/users/" + sourceUserID + "/folders/0";

        JsonObject destinationUser = new JsonObject()
            .add("id", destinationUserID);
        JsonObject ownedBy = new JsonObject()
            .add("owned_by", destinationUser);

        String result = TestConfig.getFixture("BoxFolder/PutTransferFolder200");

        wireMockRule.stubFor(WireMock.put(WireMock.urlPathEqualTo(transferContentURL))
            .withRequestBody(WireMock.equalToJson(ownedBy.toString()))
            .willReturn(WireMock.aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(result)));

        BoxUser sourceUser = new BoxUser(this.api, sourceUserID);
        BoxFolder.Info movedFolder = sourceUser.transferContent(destinationUserID);

        Assert.assertEquals(transferredFolderName, movedFolder.getName());
        Assert.assertEquals(createdByLogin, movedFolder.getCreatedBy().getLogin());
    }

    @Test(expected = BoxDeserializationException.class)
    public void testDeserializationException() throws IOException {
        final String userID = "12345";
        final String usersURL = "/2.0/users/" + userID;

        String result = TestConfig.getFixture("BoxUser/GetUserInfoCausesDeserializationException");

        wireMockRule.stubFor(WireMock.get(WireMock.urlPathEqualTo(usersURL))
            .willReturn(WireMock.aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(result)));

        BoxUser.Info userInfo = new BoxUser(this.api, userID).getInfo();
        Assert.assertEquals("12345", userInfo.getID());
    }

    @Test
    public void testCreateReadAddTrackingCodesSucceeds() throws IOException {
        final String userID = "12345";
        final String departmentID = "8675";
        final String companyID = "1701";
        final String usersURL = "/2.0/users/" + userID;

        // Mock: Create two tracking codes
        Map<String, String> createTrackingCodes = new HashMap<>();
        createTrackingCodes.put("Employee ID", userID);
        createTrackingCodes.put("Department ID", departmentID);
        String createBody = this.trackingCodesJson(createTrackingCodes).toString();
        String createResponse = TestConfig.getFixture("BoxUser/CreateTrackingCodes200");
        wireMockRule.stubFor(WireMock.put(WireMock.urlPathEqualTo(usersURL))
            .withRequestBody(WireMock.equalToJson(createBody))
            .willReturn(WireMock.aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(createResponse)));

        // Mock: Verify change
        String twoTrackingCodesResponse = TestConfig.getFixture("BoxUser/GetUserTwoTrackingCodes200");
        wireMockRule.stubFor(WireMock.get(WireMock.urlPathEqualTo(usersURL))
            .withQueryParam("fields", WireMock.equalTo("tracking_codes"))
            .inScenario("Get Tracking Code Scenario")
            .whenScenarioStateIs(Scenario.STARTED)
            .willReturn(WireMock.aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(twoTrackingCodesResponse))
            .willSetStateTo("1st Request"));

        // Mock: Add one more tracking code
        Map<String, String> appendTrackingCodes = new HashMap<>();
        appendTrackingCodes.put("Employee ID", userID);
        appendTrackingCodes.put("Department ID", departmentID);
        appendTrackingCodes.put("Company ID", companyID);
        String updateBody = this.trackingCodesJson(appendTrackingCodes).toString();
        String updateResponse = TestConfig.getFixture("BoxUser/UpdateTrackingCodes200");
        wireMockRule.stubFor(WireMock.put(WireMock.urlPathEqualTo(usersURL))
            .withRequestBody(WireMock.equalToJson(updateBody))
            .willReturn(WireMock.aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(updateResponse)));

        // Mock: Verify change
        String threeTrackingCodesResponse = TestConfig.getFixture("BoxUser/GetUserThreeTrackingCodes200");
        wireMockRule.stubFor(WireMock.get(WireMock.urlPathEqualTo(usersURL))
            .withQueryParam("fields", WireMock.equalTo("tracking_codes"))
            .inScenario("Get Tracking Code Scenario")
            .whenScenarioStateIs("1st Request")
            .willReturn(WireMock.aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(threeTrackingCodesResponse))
            .willSetStateTo("2nd Request"));

        // Create two tracking codes
        BoxUser user = new BoxUser(this.api, userID);
        BoxUser.Info info = user.new Info();
        info.setTrackingCodes(createTrackingCodes);
        user.updateInfo(info);

        // Verify change
        user = new BoxUser(this.api, userID);
        info = user.getInfo("tracking_codes");
        Map<String, String> receivedTrackingCodes = info.getTrackingCodes();
        Assert.assertEquals(createTrackingCodes, receivedTrackingCodes);

        // Add one more tracking code
        info.appendTrackingCodes("Company ID", companyID);
        user.updateInfo(info);

        // Verify change
        user = new BoxUser(this.api, userID);
        info = user.getInfo("tracking_codes");
        receivedTrackingCodes = info.getTrackingCodes();
        Assert.assertEquals(appendTrackingCodes, receivedTrackingCodes);
    }

    @Test
    public void uploadAvatarAsFile() {
        final String userID = "12345";
        BoxUser user = new BoxUser(api, userID);
        String fileName = "red_100x100.png";
        String filePath = getSampleFilePath(fileName);
        File file = new File(filePath);

        wireMockRule.stubFor(WireMock.post(WireMock.urlPathEqualTo("/2.0/users/12345/avatar"))
            .withHeader("Content-Type", new ContainsPattern("multipart/form-data"))
            .withMultipartRequestBody(
                new MultipartValuePatternBuilder()
                    .withName("pic")
                    .withHeader("Content-Type", new EqualToPattern("image/png"))
            )
            .willReturn(WireMock.aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("{\"pic_urls\": {\"small\": \"url1\",\"large\": \"url2\",\"preview\": \"url3\"}}")));

        AvatarUploadResponse response = user.uploadAvatar(file);

        assertThat(response.getSmall(), CoreMatchers.equalTo("url1"));
        assertThat(response.getLarge(), CoreMatchers.equalTo("url2"));
        assertThat(response.getPreview(), CoreMatchers.equalTo("url3"));
    }

    @Test
    public void uploadAvatarAsStream() throws IOException {
        final String userID = "12345";
        BoxUser user = new BoxUser(api, userID);
        String fileName = "red_100x100.png";
        String filePath = getSampleFilePath(fileName);

        wireMockRule.stubFor(WireMock.post(WireMock.urlPathEqualTo("/2.0/users/12345/avatar"))
            .withHeader("Content-Type", new ContainsPattern("multipart/form-data"))
            .withMultipartRequestBody(
                new MultipartValuePatternBuilder()
                    .withName("pic")
                    .withHeader("Content-Type", new EqualToPattern("image/png"))
            )
            .willReturn(WireMock.aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("{\"pic_urls\": {\"small\": \"url1\",\"large\": \"url2\",\"preview\": \"url3\"}}")));

        AvatarUploadResponse response = user.uploadAvatar(Files.newInputStream(Paths.get(filePath)), fileName);

        assertThat(response.getSmall(), CoreMatchers.equalTo("url1"));
        assertThat(response.getLarge(), CoreMatchers.equalTo("url2"));
        assertThat(response.getPreview(), CoreMatchers.equalTo("url3"));
    }

    @Test
    public void deleteAvatar() {
        final String userID = "12345";
        BoxUser user = new BoxUser(api, userID);

        wireMockRule.stubFor(WireMock.delete(WireMock.urlPathEqualTo("/2.0/users/12345/avatar"))
            .willReturn(WireMock.aResponse().withStatus(204)));

        user.deleteAvatar();
    }

    private static String getSampleFilePath(String fileName) {
        URL fileURL = BoxUserTest.class.getResource("/sample-files/" + fileName);
        try {
            return URLDecoder.decode(fileURL.getFile(), "utf-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private JsonObject trackingCodesJson(Map<String, String> trackingCodes) {
        JsonArray trackingCodesJsonArray = new JsonArray();
        for (String attrKey : trackingCodes.keySet()) {
            JsonObject trackingCode = new JsonObject();
            trackingCode.set("type", "tracking_code");
            trackingCode.set("name", attrKey);
            trackingCode.set("value", trackingCodes.get(attrKey));
            trackingCodesJsonArray.add(trackingCode);
        }

        JsonObject trackingCodesJson = new JsonObject();
        trackingCodesJson.set("tracking_codes", trackingCodesJsonArray);
        return trackingCodesJson;
    }
}
