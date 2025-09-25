Feature: Author Management API
  Validate all CRUD operations for AuthorEntity with comprehensive testing

  Background:
    Given set base url to "http://localhost:8080"
    And add to headers "Create_header"
    | Content-Type  | application/json |

  Scenario: Comprehensive CRUD operations with generated data
    # Create multiple authors with generated names
    Given set endpoint to "/authors"
    And generate random values
      | name1 | letters(12) |
      | name2 | letters(10) |
      | name3 | letters(8)  |
      | updated_name | string(8)  |

    # Create first author
    And add to JSON body "Create_body" from context
      | name        | name1          |
    And set JSON body "Create_body" with:
      #| bigNumber   | 9999999999999999999 |
      #| preciseDecimal | 123.456789012345    |
      #| complex     | {"users": [{"name": "John"}, {"name": "Jane"}]} |
      #| nestedArray | [[1,2], [3,4]]        |
      #| complexData | {"name": "John", "age": 30} |
      #| nested      | {"user": {"id": 1, "active": true}} |
      #| mixedArray  | [1, "hello", true] |
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
    And clear body "Create_body"
    And add to JSON body "Create_body2" from context
      | name        | name2          |
    And set JSON body "Create_body2" with:
      | age         | 35             |
      | active      | true           |
    When send a POST request
    Then validate status code of 201
    And extract values from response
      | $.id | author_id1 |
    And append response to file "created_author.json"

    # Create third author
    And clear body "Create_body2"
    And add to JSON body "Create_body3" from context
      | name        | name3          |
    And set JSON body "Create_body3" with:
      | age         | 40             |
      | active      | true           |
    When send a POST request
    Then validate status code of 201
    And extract values from response
      | $.id | author_id2 |
    And append response to file "created_author.json"

    # Test GET operations
    And set endpoint to "/authors/{author_id0}"
    And add to path parameters from context
    |author_id0|author_id0|
    When send a GET request
    Then validate status code of 200
    And verify response json path "$.id" equals "${author_id0}"
    And verify response json path "$.name" equals "${name1}"

    # Test PUT update
    And set endpoint to "/authors/{author_id1}"
    And add to path parameters from context
      |author_id1|author_id1|
    And clear body "Create_body3"
    And add to JSON body "Create_body4" from context
      | name        | updated_name   |  # Use underscore here
    And set JSON body "Create_body4" with:
      | age         | 36             |
      | active      | false          |
    When send a PUT request
    Then validate status code of 200
    And verify response json path "$.name" equals "${updated_name}"
      # Use underscore here
    And verify response json path "$.age" equals "36"

    # Test PATCH partial update
    And set endpoint to "/authors/{author_id2}"
    And add to path parameters from context
      |author_id2|author_id2|
    And clear body "Create_body4"
    And set JSON body "Create_body5" with:
      | age | 42 |
    When send a PATCH request
    Then validate status code of 200
    And verify response json path "$.age" equals "42"
    And verify response json path "$.name" equals "${name3}"

    # Cleanup all authors
    And set endpoint to "/authors/{author_id0}"
    And add to path parameters from context
      |author_id0|author_id0|
    When send a DELETE request
    Then validate status code of 204

    And set endpoint to "/authors/{author_id1}"
    And add to path parameters from context
      |author_id1|author_id1|
    When send a DELETE request
    Then validate status code of 204

    And set endpoint to "/authors/{author_id2}"
    And add to path parameters from context
      |author_id2|author_id2|
    When send a DELETE request
    Then validate status code of 204

  Scenario: Error handling and validation scenarios
    # Test missing required fields
    Given set endpoint to "/authors"
    When set empty JSON body "EMPTY_JSON"
    When send a POST request with body "EMPTY_JSON"
    Then validate status code of 400
    And verify response json path "$.name" equals "name must not be empty"
    And verify response json path "$.age" equals "age is required"
    And verify response json path "$.active" equals "active is required"

    # Test invalid data
    And set JSON body "Create_body6" with:
      | name   | "@Invalid" |
      | age    | 126        |
      | active | null       |
    When send a POST request
    Then validate status code of 400
    And verify response json path "$.name" equals "name must not contain special characters"
    And verify response json path "$.age" equals "age must not exceed 125"
    And verify response json path "$.active" equals "active is required"

    # Test duplicate author creation
    And clear body "Create_body6"
    And generate random values
      | dup_name | letters(15) |

    And add to JSON body "Create_body7" from context
      | name    | dup_name |
    And set JSON body "Create_body7" with:
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
    And add to path parameters from context
      |dup_author_id|dup_author_id|
    When send a DELETE request
    Then validate status code of 204

    # Test non-existent author operations - GET
    And set endpoint to "/authors/999999"
    When send a GET request
    Then validate status code of 404
    And verify response json path "$.error" equals "Author not found"

    # Test non-existent author operations - PUT
    And set JSON body "Create_body8" with:
      | name   | Fail Update |
      | age    | 50          |
      | active | true        |
    When send a PUT request
    Then validate status code of 404
    And verify response json path "$.error" equals "Author not found"

    # Test non-existent author operations - PATCH
    And clear body "Create_body8"
    And set JSON body "Create_body9" with:
      | age | 55 |
    When send a PATCH request
    Then validate status code of 404
    And verify response json path "$.error" equals "Author not found"

    # Test non-existent author operations - DELETE
    When send a DELETE request
    Then validate status code of 404
    And verify response json path "$.error" equals "Author not found"