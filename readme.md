# 🚀 API Automation Framework with Cucumber & Allure
#### A comprehensive API testing framework built with Cucumber, TestNG, Apache HTTP Client, and Allure Reports for robust REST API testing with beautiful reporting.

## 📋 Framework Overview
#### This framework provides a complete solution for API testing with:

1. BDD-style tests using Cucumber Gherkin syntax
2. Multiple authentication methods support
3. Data-driven testing with CSV/JSON support
4. JSON Schema validation for response contracts
5. Performance monitoring with response time tracking
6. Beautiful Allure reports with request/response capture
7. Comprehensive logging with configurable levels

## 🏗️ Architecture
text
src/
├── main/java/api/
│   ├── client/          # HTTP client implementation
│   ├── config/          # Configuration management
│   ├── helpers/         # Utility classes (JSON, extraction, etc.)
│   ├── hooks/           # Cucumber hooks and test runner
│   ├── reporting/       # Allure reporting integration
│   ├── performance/     # Performance monitoring
│   └── auth/           # Authentication management
└── test/resources/
├── features/        # Cucumber feature files
├── config/          # Environment configuration files
└── schemas/         # JSON schema files

## 🚀 Quick Start
### Prerequisites
   1. Java 17+
   2. Maven 3.6+
   3. Allure CLI (for report generation)

### Installation
   1. git clone <your-repo>
   2. cd APIFrameworkCucumber
   3. mvn clean install
   4. Run Tests

### Run all tests
mvn test

### Run with specific environment
mvn test -Denv=prod

### Run with debug logging
mvn test -Dlogging.debug=true

### Generate Allure report
mvn allure:serve

#### Generate and view Allure report
mvn allure:serve

### Or generate report only
mvn allure:report

## 📝 Writing Tests
Sample Feature File
```
gherkin
Feature: User API Tests
Scenario: Create and retrieve user
Given set base url to "https://api.example.com"
And add to headers
| Content-Type  | application/json |
| Authorization | Bearer token123  |

    Given set endpoint to "/users"
    And add to body
      | name  | John Doe |
      | email | john@example.com |
    
    When send a POST request
    Then validate status code of 201
    And extract values from response
      | $.id | user_id |
    
    And set endpoint to "/users/{user_id}"
    When send a GET request
    Then validate status code of 200
    And verify response json path "$.name" equals "John Doe"
```
### Step Definitions Available
#### Setup Steps:

1. set base url to {string}
2. add to headers (data table)
3. set endpoint to {string}
4. apply authentication {string}

#### Data Management:

1. generate random values (data table)
2. add to body / add to body from context
3. load test data from {string}
4. use test data row {int}

#### HTTP Operations:

1. send a {method} request
2. send a {method} request with {int} retries
3. send a {method} request and measure performance

#### Validations:

1. validate status code of {int}
2. extract values from response
3. verify response schema {string}
4. verify response json path {string} equals {string}
5. verify response contains {string}

#### ⚙️ Configuration
#####  Environment Configuration
###### Create property files in src/test/resources/config/:
```
config/api-dev.properties:

properties
base.url=https://dev-api.example.com
timeout.seconds=30
max.retries=3
logging.debug=true

# Authentication
auth.basic.username=testuser
auth.basic.password=testpass
auth.bearer.token=dev_token_123
config/api-prod.properties:

properties
base.url=https://api.example.com
timeout.seconds=15
max.retries=1
logging.debug=false
Running with Different Environments

```
```
mvn test -Denv=dev      # Development environment
mvn test -Denv=qa       # QA environment  
mvn test -Denv=prod     # Production environment
```
#### 🔧 Framework Components
#####  AsyncRestClient 
1. Non-blocking HTTP client using Apache HTTP Async Client
2. Automatic retry mechanism
3. Request/response interception
4. Connection pooling

##### PathExtractor
1. JSONPath support for response extraction
2. Variable storage and substitution
3. Placeholder resolution in URLs and assertions

##### DataDrivenTestGenerator
1. Load test data from CSV/JSON files
2. Support for multiple data formats
3. Row-based test data management

##### EnhancedAllureReporter
1. Automatic request/response capture
2. cURL command generation
3. Beautiful HTTP attachment formatting

##### PerformanceMonitor
1. Response time tracking
2. Performance assertions
3. Statistical reporting

#### 🎨 Allure Reporting
The framework automatically captures:

1. ✅ HTTP Request details (URL, method, headers, body)
2. ✅ HTTP Response details (status, headers, body)
3. ✅ cURL commands for debugging
4. ✅ Performance metrics
5. ✅ Extracted values and test data
6. ✅ Screenshots (if added)