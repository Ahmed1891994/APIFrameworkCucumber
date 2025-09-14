Feature: Author Management API
  Validate all CRUD operations for AuthorEntity with comprehensive testing

  Background:
    Given set base url to "http://localhost:8080"
    And add to headers
      | Content-Type  | application/json |

  Scenario: Comprehensive CRUD operations with generated data
    # Create multiple authors with generated names
    Given set endpoint to "/authors"
    And generate random values
      | name1 | string(12) |
      | name2 | string(10) |
      | name3 | string(8)  |
      | updated_name | string(8)  |

    # Create first author
    And add to body from context
      | name        | name1          |
    And add to body
      | age         | 45             |
      | active      | true           |
      | rating      | 4.5            |
      | totalBooks  | 3              |
      | genres      | ["Fiction","Mystery"] |
      | scores      | [90,85,78]     |
      | wealth      | 100000.50    |
      | followers   | 1500         |
    When send a POST request
    Then validate status code of 201
    And extract values from response
      | $.id | author_id0 |
    And save response to file "created_author.json"

    # Create second author
    And clear current body
    And add to body from context
      | name        | name2          |
    And add to body
      | age         | 35             |
      | active      | true           |
    When send a POST request
    Then validate status code of 201
    And extract values from response
      | $.id | author_id1 |
    And append response to file "created_author.json"

    # Create third author
    And clear current body
    And add to body from context
      | name        | name3          |
    And add to body
      | age         | 40             |
      | active      | true           |
    When send a POST request
    Then validate status code of 201
    And extract values from response
      | $.id | author_id2 |
    And append response to file "created_author.json"

    # Test GET operations
    And set endpoint to "/authors/{author_id0}"
    When send a GET request
    Then validate status code of 200
    And verify response json path "$.id" equals "${author_id0}"
    And verify response json path "$.name" equals "${name1}"

    # Test PUT update
    And set endpoint to "/authors/{author_id1}"
    And clear current body
    And add to body from context
      | name        | updated_name   |  # Use underscore here
    And add to body
      | age         | 36             |
      | active      | false          |
    When send a PUT request
    Then validate status code of 200
    And verify response json path "$.name" equals "${updated_name}"
      # Use underscore here
    And verify response json path "$.age" equals "36"

    # Test PATCH partial update
    And set endpoint to "/authors/{author_id2}"
    And clear current body
    And add to body
      | age | 42 |
    When send a PATCH request
    Then validate status code of 200
    And verify response json path "$.age" equals "42"
    And verify response json path "$.name" equals "${name3}"

    # Cleanup all authors
    And set endpoint to "/authors/{author_id0}"
    When send a DELETE request
    Then validate status code of 204

    And set endpoint to "/authors/{author_id1}"
    When send a DELETE request
    Then validate status code of 204

    And set endpoint to "/authors/{author_id2}"
    When send a DELETE request
    Then validate status code of 204

  Scenario: Error handling and validation scenarios
    # Test missing required fields
    Given set endpoint to "/authors"
    And clear current body
    When send a POST request
    Then validate status code of 400
    And verify response json path "$.name" equals "name must not be empty"
    And verify response json path "$.age" equals "age is required"
    And verify response json path "$.active" equals "active is required"

    # Test invalid data
    And clear current body
    And add to body
      | name   | "@Invalid" |
      | age    | 126        |
      | active | null       |
    When send a POST request
    Then validate status code of 400
    And verify response json path "$.name" equals "name must not contain special characters"
    And verify response json path "$.age" equals "age must not exceed 125"
    And verify response json path "$.active" equals "active is required"

    # Test duplicate author creation
    And clear current body
    And generate random values
      | dup_name | string(15) |

    And add to body from context
      | name    | dup_name |
    And add to body
      | active  | true        |
      | age     | 40          |
    When send a POST request
    Then validate status code of 201
    And extract values from response
      | $.id | dup_author_id |

    # Try to create duplicate - body should still contain the same data
    When send a POST request
    Then validate status code of 409
    And verify response json path "$.error" equals "Author with this name already exists"

    # Cleanup duplicate author
    And set endpoint to "/authors/{dup_author_id}"
    When send a DELETE request
    Then validate status code of 204

    # Test non-existent author operations - GET
    And set endpoint to "/authors/999999"
    When send a GET request
    Then validate status code of 404
    And verify response json path "$.error" equals "Author not found"

    # Test non-existent author operations - PUT
    And clear current body
    And add to body
      | name   | Fail Update |
      | age    | 50          |
      | active | true        |
    When send a PUT request
    Then validate status code of 404
    And verify response json path "$.error" equals "Author not found"

    # Test non-existent author operations - PATCH
    And clear current body
    And add to body
      | age | 55 |
    When send a PATCH request
    Then validate status code of 404
    And verify response json path "$.error" equals "Author not found"

    # Test non-existent author operations - DELETE
    When send a DELETE request
    Then validate status code of 404
    And verify response json path "$.error" equals "Author not found"