
Feature: API Testing with Schema Validation

  Background:
    Given set base url to "https://reqres.in"
    And add to headers
      | x-api-key | reqres-free-v1 |
      | Accept    | application/json |
    And set endpoint to "/api/users"
    And generate random values
      | job | jobId  |
    And add to body from context
      | job | job |
    And add to body
      | name | Ahmed |
    When send a POST request
    Then validate status code of 201
    Then extract values from response
      | $.id | id |

  Scenario: Get by Id
    And set endpoint to "/{id}"
    And add to path parameters from context
      | id | id |
    When send a GET request
    Then validate status code of 200
    Then verify response schema "user_get"