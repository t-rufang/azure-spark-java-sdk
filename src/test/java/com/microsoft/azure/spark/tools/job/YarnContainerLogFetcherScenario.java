// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.azure.spark.tools.job;

import com.microsoft.azure.spark.tools.clusters.YarnCluster;
import com.microsoft.azure.spark.tools.http.AmbariHttpObservable;
import com.microsoft.azure.spark.tools.http.HttpObservable;
import com.microsoft.azure.spark.tools.utils.LaterInit;
import com.microsoft.azure.spark.tools.utils.MockHttpService;
import cucumber.api.java.After;
import cucumber.api.java.Before;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import uk.org.lidalia.slf4jtest.TestLogger;
import uk.org.lidalia.slf4jtest.TestLoggerFactory;

import java.net.URI;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class YarnContainerLogFetcherScenario {
    private HttpObservable httpMock;
    private LaterInit<Integer> batchIdMock;
    private Throwable caught;
    private MockHttpService httpServerMock;
    private YarnCluster yarnClusterMock;
    private YarnContainerLogFetcher yarnDriverLogFetcherMock;
    private TestLogger logger = TestLoggerFactory.getTestLogger(YarnContainerLogFetcher.class);

    @Before("@YarnContainerLogFetcherScenario")
    public void setUp() throws Throwable {
        httpMock = new AmbariHttpObservable();
        caught = null;
        this.httpServerMock = MockHttpService.create();
    }

    @After("@YarnContainerLogFetcherScenario")
    public void cleanUp(){
        this.httpServerMock.stop();
        TestLoggerFactory.clear();
    }

    @Given("^setup a mock Yarn service for (.+) request '(.+)' to return '(.+)' with status code (\\d+)$")
    public void mockLivyService(String action, String serviceUrl, String response, int statusCode) {
        httpServerMock.stub(action, serviceUrl, statusCode, response);
    }

    @Given("^prepare a Yarn cluster with Node Manager base URL (\\S*) and UI base URL (\\S*)$")
    public void prepareAYarnClusterWithNodeManagerBaseURLAndUIBaseURL(String nmBaseMock, String uiBaseMock) {
        yarnClusterMock = mock(YarnCluster.class);
        when(yarnClusterMock.getYarnNMConnectionUrl()).thenReturn(httpServerMock.normalizeResponse(nmBaseMock));
        when(yarnClusterMock.getYarnUIBaseUrl()).thenReturn(httpServerMock.normalizeResponse(uiBaseMock));
    }

    @Given("^create a yarn application driver with id (.+)$")
    public void createAYarnApplicationDriverWithId(String appIdMock) {
        yarnDriverLogFetcherMock = new YarnContainerLogFetcher(appIdMock, yarnClusterMock, httpMock);
    }

    @Then("^Parsing driver HTTP address '(.+)' should get host '(.+)'$")
    public void checkParsingDriverHTTPAddressHost(
            String httpAddress,
            String expectedHost) {
        assertEquals(expectedHost, yarnDriverLogFetcherMock.parseAmHostHttpAddressHost(httpAddress));
    }

    @Then("^Parsing driver HTTP address '(.+)' should be null$")
    public void checkParsingDriverHTTPAddressHostFailure(String httpAddress) {
        assertNull(yarnDriverLogFetcherMock.parseAmHostHttpAddressHost(httpAddress));
    }

    @Then("^getting Spark driver host should be '(.+)'$")
    public void checkGetSparkDriverHost(String expectedHost) {
        try {
            assertEquals(expectedHost, yarnDriverLogFetcherMock.getDriverHost().toBlocking().single());
        } catch (Exception e) {
            caught = e.getCause();
            assertEquals("Shouldn't get " + e, expectedHost, "__exception_got__");
        }
    }

    @Then("^getting current Yarn App attempt should be '(.+)'$")
    public void checkGetCurrentYarnAppAttemptResult(String appAttemptLogsUrlExpect) {
        URI appAttemptLogsLink = yarnDriverLogFetcherMock
                .getSparkJobYarnCurrentAppAttemptLogsLink()
                .toBlocking()
                .first();

        assertEquals(appAttemptLogsUrlExpect, appAttemptLogsLink.toString());
    }

    @Then("^getting Spark Job driver log URL Observable should be '(.+)'$")
    public void checkSparkJobDriverLogURLObservable(String expect) {
        URI url = yarnDriverLogFetcherMock.getSparkJobDriverLogUrl().toBlocking().last();

        assertEquals(httpServerMock.normalizeResponse(expect), url.toString());
    }

    @Then("^getting Spark Job driver log URL Observable should be empty$")
    public void gettingSparkJobDriverLogURLObservableShouldBeEmpty() throws Throwable {
        assertTrue(yarnDriverLogFetcherMock.getSparkJobDriverLogUrl().isEmpty().toBlocking().last());
    }
}
