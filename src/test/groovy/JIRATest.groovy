import org.apache.http.client.HttpClient
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.HttpException;
import org.apache.http.entity.StringEntity;
import org.apache.http.HttpResponse
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import org.json.JSONObject;
import org.json.JSONTokener;
import org.json.JSONArray;

public class TestJIRAHelper {

    private String username;
    private String password;
    private String serverURL;
    private String authToken;

    public TestJIRAHelper(String username, String password, String serverURL) {
        this.username = username;
        this.password = password;
        if (serverURL.endsWith("/")) {
            this.serverURL = serverURL;
        } else {
            this.serverURL = serverURL + "/";
        }
        String creds = username+':'+password;
        this.authToken = "Basic " + creds.bytes.encodeBase64().toString()
        createSession()
    }

    /**
     * Pre authenticate and creates a session for the JIRA user
     */
    private void createSession() {
        HttpClient client = new DefaultHttpClient();
        HttpGet get = new HttpGet(this.serverURL + "rest/auth/latest/session")
        get.addHeader("Authorization", this.authToken);
        get.addHeader("accept", "application/json");

        HttpResponse response;
        try {
            response = client.execute(get);
        } catch (HttpException e) {
            exitFailure("There was an error executing the request.");
        }

        if (!(response.getStatusLine().getStatusCode() != "200"))
            httpFailure(response);
    }

    /**
     * Executes the given HTTP request and checks for a correct response status
     * @param request The HttpRequest to execute
     * @param expectedStatus The response status that indicates a successful request
     * @param body The JSONObject containing the request body
     * @return A JSONObject containing the response to the HTTP request executed
     */
    private HttpResponse executeHttpRequest(Object request, int expectedStatus, JSONObject body) {
        // Make sure the required parameters are there
        if ((request == null) || (expectedStatus == null)) exitFailure("An error occurred executing the request.");

        println ">>>Sending request: ${request}"
        if (body != null) "\n>>>Body contents:\n${body}";
        HttpClient client = new DefaultHttpClient();
        request.setHeader("Authorization", this.authToken);
        if (body) {
            StringEntity input = new StringEntity(body.toString());
            input.setContentType("application/json");
            request.setEntity(input);
        }

        HttpResponse response;
        try {
            response = client.execute(request);
        } catch (HttpException e) {
            exitFailure("There was an error executing the request.");
        }

        if (!(response.getStatusLine().getStatusCode() == expectedStatus))
            httpFailure(response);

        println ">>>Received the response:"
        println response.getStatusLine();
        return response;
    }

    /**
     * Write an error message to console and exit on a fail status.
     * @param message The error message to write to the console.
     */
    private void exitFailure(String message) {
        println "${message}";
        System.exit(1);
    }

    /**
     * Write a HTTP error message to console and exit on a fail status.
     * @param message The error message to write to the console.
     */
    private void httpFailure(HttpResponse response) {
        println "Request failed : " + response.getStatusLine();
        String responseString = new BasicResponseHandler().handleResponse(response);
        println "${responseString}";
        System.exit(1);
    }

    /**
     * Prints the key and value for a server instance property
     * @param key The key for the object being printed
     * @param val The value for the object being printed
     */
    private void printProps(String key, String val) {
        println "Key: " + key + " has value: " + val;
    }

    /**
     * Create a new JIRA issue
     */
    public void createIssue(String fields, String comps, String vers) {
        HttpPost post = new HttpPost(this.serverURL + "rest/api/latest/issue");

        JSONObject issue = new JSONObject();
        JSONObject details = new JSONObject();

        // project
        JSONObject project = new JSONObject();
        project.put("key", "DEMO")
        details.put("project", project);
        // issueType
        JSONObject issueType = new JSONObject();
        issueType.put("name", "Bug")
        details.put("issuetype", issueType);
        // priority
        JSONObject priority = new JSONObject();
        priority.put("name", "Critical") ;
        details.put("priority", priority);
        // components
        JSONArray components = new JSONArray();
        comps.split(',').collect {
            JSONObject component = new JSONObject();
            component.put("name", it);
            components.put(component);
        }
        details.put("components", components);
        // versions
        JSONArray versions = new JSONArray();
        vers.split(',').collect {
            JSONObject version = new JSONObject();
            version.put("name", it);
            versions.put(version);
        }
        details.put("versions", versions);

        details.put("summary", "this is a summary");
        details.put("description", "this is the description");

        // additional fields
        fields.split('\n').collect {
            def (fldName, fldVal) = it.tokenize('=');
            details.put(fldName, fldVal);
        }
        issue.put("fields", details);

        HttpResponse response = executeHttpRequest(post, 201, issue);
        BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
        String json = reader.readLine();
        JSONObject jsonResponse = new JSONObject(new JSONTokener(json));
        println "Successfully created issue with key: " + jsonResponse.getString("key");
        println "See " + jsonResponse.getString("self") + " for more information";
    }



    /**
     * Edit an existing JIRA issue
     */
    public void editIssue(String issId, String fields, String comps, String vers) {
        def issueId = issId;
        if (!issueExists(issueId)) {
            exitFailure("Could not find issue with key: ${issueId}");
        }

        HttpPut put = new HttpPut(this.serverURL + "rest/api/latest/issue/" + issueId);

        JSONObject issue = new JSONObject();
        JSONObject update = new JSONObject();
        JSONObject details = new JSONObject();

        // issueType
        JSONObject issueType = new JSONObject();
        issueType.put("name", "New Feature")
        details.put("issuetype", issueType);
        // priority
        JSONObject priority = new JSONObject();
        priority.put("name", "Major") ;
        details.put("priority", priority);

        // components
        JSONObject componentsUpdate = new JSONObject();
        JSONArray componentsSet = new JSONArray()
        JSONArray components = new JSONArray();
        comps.split(',').collect {
            JSONObject component = new JSONObject();
            component.put("name", it);
            components.put(component);
        }
        componentsUpdate.put("set",components)
        componentsSet.put(componentsUpdate)
        update.put("components", componentsSet);
        // versions
        JSONObject versionsUpdate = new JSONObject();
        JSONArray versionsSet = new JSONArray()
        JSONArray versions = new JSONArray();
        vers.split(',').collect {
            JSONObject version = new JSONObject();
            version.put("name", it);
            versions.put(version);
        }
        versionsUpdate.put("set",versions)
        versionsSet.put(versionsUpdate)
        update.put("versions", versionsSet);

        // comments
        JSONObject commentsUpdate = new JSONObject();
        JSONArray commentsSet = new JSONArray()
        JSONObject comment = new JSONObject();
        comment.put("body", "a new comment again");
        commentsUpdate.put("add",comment)
        commentsSet.put(commentsUpdate)
        update.put("comment", commentsSet);

        issue.put("update", update);

        details.put("summary", "this is a summary updated");
        details.put("description", "this is the description updated");

        // additional fields
        fields.split('\n').collect {
            def (fldName, fldVal) = it.tokenize('=');
            details.put(fldName, fldVal);
        }
        issue.put("fields", details);

        println issue.toString();

        HttpResponse response = executeHttpRequest(put, 204, issue);
        //BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
        //String json = reader.readLine();
        //JSONObject jsonResponse = new JSONObject(new JSONTokener(json));
        println "Successfully updated issue with key: ${issueId}";
    }

    /**
     * Edit a list of JIRA Issues
     */
    public void editIssues(String comments, String issIds) {
        def issueIds = issIds.split(',') as List;
        for (def issueId : issueIds.sort()) {
            if (issueExists(issueId)) {
                editIssue()
            } else {
                /*if (failMode != FailMode.WARN_ONLY) {
                    exitFailure("Error: issue with key " + issueId + " not found.")
                }*/
            }
        }
    }

    /**
     * Edit an existing JIRA issue
     */
    public void transitionIssue(String issId, String vers, String transName) {
        def issueId = issId;
        if (!issueExists(issueId)) {
            exitFailure("Could not find issue with key: ${issueId}");
        }

        HttpPost post = new HttpPost(this.serverURL + "rest/api/latest/issue/" + issueId + "/transitions");

        JSONObject issue = new JSONObject();
        JSONObject transition = new JSONObject();
        JSONObject update = new JSONObject();
        JSONObject details = new JSONObject();

        // transition
        def transitionId = getTransitionId(issueId, transName)
        transition.put("id", transitionId)
        issue.put("transition", transition)

        // fix versions
        JSONObject versionsUpdate = new JSONObject();
        JSONArray versionsSet = new JSONArray()
        JSONArray versions = new JSONArray();
        vers.split(',').collect {
            JSONObject version = new JSONObject();
            version.put("name", it);
            versions.put(version);
        }
        versionsUpdate.put("set",versions)
        versionsSet.put(versionsUpdate)
        update.put("fixVersions", versionsSet);

        // comments
        JSONObject commentsUpdate = new JSONObject();
        JSONArray commentsSet = new JSONArray()
        JSONObject comment = new JSONObject();
        comment.put("body", "a new comment");
        commentsUpdate.put("add",comment)
        commentsSet.put(commentsUpdate)
        update.put("comment", commentsSet);

        issue.put("update", update);

        JSONObject assignee = new JSONObject();
        assignee.put("name", "admin")
        details.put("assignee", assignee)

        JSONObject resolution = new JSONObject();
        resolution.put("name", "Fixed")
        details.put("resolution", resolution)

        issue.put("fields", details);

        HttpResponse response = executeHttpRequest(post, 204, issue);
        println "Successfully transitioned issue with key: ${issueId}";
    }

    /**
     * Add comments to a list of JIRA Issues
     */
    public void addCommentsToIssues(String comments, String issIds) {
        def issueIds = issIds.split(',') as List;
        for (def issueId : issueIds.sort()) {
            if (issueExists(issueId)) {
                HttpPost post = new HttpPost(this.serverURL + "rest/api/latest/issue/${issueId}/comment?expand=renderedBody");
                JSONObject comment = new JSONObject();
                comment.put("body", comments);
                HttpResponse response = executeHttpRequest(post, 201, comment);
                println "Successfully added comment to issue with key: ${issueId}";
            } else {
                /*if (failMode != FailMode.WARN_ONLY) {
                    exitFailure("Error: issue with key " + issueId + " not found.")
                }*/
            }
        }
    }

    public boolean projectExists(String projectKey) {
        try {
            HttpGet get = new HttpGet(this.serverURL + "rest/api/latest/project/" + projectKey);
            executeHttpRequest(get, 200, null);
            return true;
        } catch (HttpResponseException ex) {
            exitFailure("The project with key ${projectKey} does not exist, or is not visible to the user");
            return false;
        }
    }

    public boolean issueExists(String issueKey) {
        try {
            HttpGet get = new HttpGet(this.serverURL + "rest/api/latest/issue/" + issueKey);
            executeHttpRequest(get, 200, null);
            return true;
        } catch (HttpResponseException ex) {
            exitFailure("The issue with key ${issueKey} does not exist, or is not visible to the user");
            return false;
        }
    }

    public String issueStatus(String issueKey) {
        try {
            HttpGet get = new HttpGet(this.serverURL + "rest/api/latest/issue/" + issueKey + "?fields=status");
            HttpResponse response = executeHttpRequest(get, 200, null);
            BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            String json = reader.readLine();
            JSONObject jsonResponse = new JSONObject(new JSONTokener(json));
            String status = jsonResponse.getJSONObject("fields").getJSONObject("status").getString("name");
            return status;
        } catch (HttpResponseException ex) {
            exitFailure("The issue with key ${issueKey} does not exist, or is not visible to the user");
            return "-1";
        }
    }

    public boolean issueTypeExists(String issueTypeName) {
         try {
            HttpGet get = new HttpGet(this.serverURL + "rest/api/latest/issuetype/");
            HttpResponse response = executeHttpRequest(get, 200, null);
            BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            String json = reader.readLine();
            JSONArray jsonResponse = new JSONArray(new JSONTokener(json));
            for (int i = 0; i < jsonResponse.length(); i++) {
                if (jsonResponse.getJSONObject(i).getString("name") == issueTypeName) {
                    return true;
                };
            }
            exitFailure("The issuetype with name ${issueTypeName} does not exist, or is not visible to the user");
            return false;
        } catch (HttpResponseException ex) {
            exitFailure("The issuetype with name ${issueTypeName} does not exist, or is not visible to the user");
            return false;
        }
    }

    public boolean priorityExists(String priorityName) {
        try {
            HttpGet get = new HttpGet(this.serverURL + "rest/api/latest/priority/");
            HttpResponse response = executeHttpRequest(get, 200, null);
            BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            String json = reader.readLine();
            JSONArray jsonResponse = new JSONArray(new JSONTokener(json));
            for (int i = 0; i < jsonResponse.length(); i++) {
                if (jsonResponse.getJSONObject(i).getString("name") == priorityName) {
                    return true;
                };
            }
            exitFailure("The priority with name ${priorityName} does not exist, or is not visible to the user");
            return false;
        } catch (HttpResponseException ex) {
            exitFailure("The priority with name ${priorityName} does not exist, or is not visible to the user");
            return false;
        }
    }

    public boolean componentExists(String projectKey, String componentName) {
        try {
            HttpGet get = new HttpGet(this.serverURL + "rest/api/latest/project/${projectKey}/components/");
            HttpResponse response = executeHttpRequest(get, 200, null);
            BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            String json = reader.readLine();
            JSONArray jsonResponse = new JSONArray(new JSONTokener(json));
            for (int i = 0; i < jsonResponse.length(); i++) {
                if (jsonResponse.getJSONObject(i).getString("name") == componentName) {
                    return true;
                };
            }
            exitFailure("The component with name ${componentName} does not exist, or is not visible to the user");
            return false;
        } catch (HttpResponseException ex) {
            exitFailure("The component with name ${componentName} does not exist, or is not visible to the user");
            return false;
        }
    }

    public boolean versionExists(String projectKey, String versionName) {
        try {
            HttpGet get = new HttpGet(this.serverURL + "rest/api/latest/project/${projectKey}/versions/");
            HttpResponse response = executeHttpRequest(get, 200, null);
            BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            String json = reader.readLine();
            JSONArray jsonResponse = new JSONArray(new JSONTokener(json));
            for (int i = 0; i < jsonResponse.length(); i++) {
                if (jsonResponse.getJSONObject(i).getString("name") == versionName) {
                    return true;
                };
            }
            exitFailure("The version with name ${versionName} does not exist, or is not visible to the user");
            return false;
        } catch (HttpResponseException ex) {
            exitFailure("The versions with name ${versionName} does not exist, or is not visible to the user");
            return false;
        }
    }

    public String getTransitionId(String issueId, String transitionName) {
        String transitionId = null;
        try {
            HttpGet get = new HttpGet(this.serverURL + "rest/api/latest/issue/${issueId}/transitions");
            HttpResponse response = executeHttpRequest(get, 200, null);
            BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            String json = reader.readLine();
            JSONObject jsonResponse = new JSONObject(new JSONTokener(json));
            JSONArray transitions = jsonResponse.getJSONArray("transitions");
            for (int i = 0; i < transitions.length(); i++) {
                if (transitions.getJSONObject(i).getString("name") == transitionName) {
                    transitionId = transitions.getJSONObject(i).getString("id");
                    println "Found transition id '${transitionId}' for transition: ${transitionName}";
                    break;
                };
            }
            if (transitionId == null)
                exitFailure("A transition with name ${transitionName} does not exist, or is not currently available to the user");
        } catch (HttpResponseException ex) {
            exitFailure("A transition with name ${transitionName} does not exist, or is not currently available to the user");
        }
        return transitionId;
    }

    /**
     * Create a new JIRA component
     */
    public void createComponent(String componentName) {
        HttpPost post = new HttpPost(this.serverURL + "rest/api/latest/component");

        JSONObject component = new JSONObject();
        component.put("name", componentName);
        component.put("description", "this is the description");
        component.put("project", "DEMO");

        executeHttpRequest(post, 201, component);

        // get version key by getting project versions
        HttpGet get = new HttpGet(this.serverURL + "rest/api/latest/project/DEMO/components");
        HttpResponse response = executeHttpRequest(get, 200, null);
        BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
        String json = reader.readLine();
        JSONArray jsonResponse = new JSONArray(new JSONTokener(json));
        for (int i = 0; i < jsonResponse.length(); i++) {
            if (jsonResponse.getJSONObject(i).getString("name") == componentName) {
                String componentId = jsonResponse.getJSONObject(i).getString("id");
                String componentUrl = jsonResponse.getJSONObject(i).getString("self");
                println "Found component ${componentName} with id ${componentId} @ ${componentUrl}";
            };
        }

        println "Successfully created component ${componentName}";

        /*
         "description": "An excellent version",
    "name": "New Version 1",
    "archived": false,
    "released": true,
    "releaseDate": "2010-07-06",
    "userReleaseDate": "6/Jul/2010",
    "project": "PXA",
    "projectId": 10000

         */
    }

    /**
     * Create a new JIRA version
     */
    public void createVersion(String versionName) {
        HttpPost post = new HttpPost(this.serverURL + "rest/api/latest/version");

        JSONObject version = new JSONObject();
        version.put("name", versionName);
        version.put("description", "this is the description");
        version.put("project", "DEMO");

        executeHttpRequest(post, 201, version);

        // get version key by getting project versions
        HttpGet get = new HttpGet(this.serverURL + "rest/api/latest/project/DEMO/versions");
        HttpResponse response = executeHttpRequest(get, 200, null);
        BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
        String json = reader.readLine();
        JSONArray jsonResponse = new JSONArray(new JSONTokener(json));
        for (int i = 0; i < jsonResponse.length(); i++) {
            if (jsonResponse.getJSONObject(i).getString("name") == versionName) {
                String versionId = jsonResponse.getJSONObject(i).getString("id");
                String versionUrl = jsonResponse.getJSONObject(i).getString("self");
                println "Found version ${versionName} with id ${versionId} @ ${versionUrl}";
            };
        }

        println "Successfully created version ${versionName}";

        /*
         "description": "An excellent version",
    "name": "New Version 1",
    "archived": false,
    "released": true,
    "releaseDate": "2010-07-06",
    "userReleaseDate": "6/Jul/2010",
    "project": "PXA",
    "projectId": 10000

         */
    }
}

TestJIRAHelper jiraTest = new TestJIRAHelper("admin", "admin", "http://localhost:6060");
def fields = """environment=an example environment
"""
def components = "Database"
def versions = "1.0"
//jiraTest.projectExists("DEMO")
//jiraTest.versionExists("DEMO", "1.0")
//jiraTest.createIssue(fields, components, versions);
//jiraTest.addCommentsToIssues("this is an <b>example</b> comment", "DEM-2,DEMO-21")
//jiraTest.editIssue("DEMO-21", fields, components, versions);
//println jiraTest.getTransitionId("DEMO-21", "Start Progress")
//jiraTest.transitionIssue("DEMO-21", "1.1", "Resolve Issue");
//println jiraTest.issueStatus("DEMO-21")
//jiraTest.checkIssueStatus("DEMO-21", "Resolved");
jiraTest.createComponent("myComponent");
