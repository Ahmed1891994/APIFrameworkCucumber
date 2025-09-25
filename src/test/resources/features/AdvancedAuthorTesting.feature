Feature: Advanced Author Testing
  Test all step definitions including random data, context manipulation, data loading, and performance

  Background:
    Given set base url to "http://localhost:8080"
    And add to headers
      | Content-Type  | application/json |

  Scenario: Advanced data manipulation and context operations
    # Test random data generation and context manipulation
    Given set endpoint to "/authors"
    And generate random values
      | name      | letters(10)        |
      | age       | [2-7]{2}          |
      | email     | email             |
      | phone     | phone             |
      | uuid      | uuid              |
      | numericId | \d{5}             |
      | rangeVal  | [0-5]{3}          |

    And add to JSON body "Create_body" from context
      | name   | name      |
      | age    | age       |
      | email  | email     |
      | phone  | phone     |
      | uuid   | uuid      |

    And set JSON body "Create_body" with:
      | active     | true               |
      | rating     | 4.5                |
      | totalBooks | 10                 |
      | genres     | ["Fiction","Tech"] |
      | extraField | should_be_removed  |

    And remove from JSON body "Create_body" keys
      | extraField |

    And set JSON body "Create_body" with:
      | followers | 500 |

    When send a POST request
    Then validate status code of 201
    And verify last request response time is less than 1000 ms
    And verify response json path "$.name" equals "${name}"
    And verify response json path "$.age" equals "${age}"
    And verify response json path "$.extraField" not exists
    And verify response schema "author_schema"

    And extract values from response
      | $.id       | author_id          |
      | $.name     | saved_name         |
      | $.age      | saved_age          |


    # Test body clearing and context-based updates
    And set endpoint to "/authors/{author_id}"
    And clear body "Create_body"
    And generate random values
      | updatedName | letters(12) |

    And add to JSON body "Create_body" from context
      | name | updatedName |

    And set JSON body "Create_body" with:
      | age | 45 |
      | active | true  |
    When send a PUT request
    Then validate status code of 200
    And verify response json path "$.name" equals "${updatedName}"
    And verify response json path "$.age" equals "45"

    # Test performance monitoring
    Then validate status code of 200
    And verify last request response time is less than 400 ms

    # Test request retries
    And set endpoint to "/authors/99999"
    When send a GET request with 2 retries
    Then validate status code of 404

    # Cleanup
    And set endpoint to "/authors/{author_id}"
    When send a DELETE request
    Then validate status code of 204

  Scenario: Data-driven testing from external sources
    # Test CSV data loading
    Given load test data from "testdata/authors.csv"
    And generate random values
      | name1 | letters(12) |
      | name2 | letters(12) |
      | name3 | letters(12) |
      | name4 | letters(12) |
    And use test data row 1

    And set endpoint to "/authors"
    And add to JSON body "Create_body" from context
      | name        | name1         |
      | age         | csv_age       |
      | active      | csv_active    |
      | rating      | csv_rating    |
      | totalBooks  | csv_totalBooks|
      | genres      | csv_genres    |
      | scores      | csv_scores    |
      | wealth      | csv_wealth    |
      | followers   | csv_followers |
    When send a POST request
    Then validate status code of 201
    And verify response json path "$.name" equals "${name1}"
    And verify response json path "$.age" equals "${csv_age}"

    And extract values from response
      | $.id | csv_author_id |

    # Test JSON data loading
    Given load test data from "testdata/authors.json"
    And use test data row 4

    And clear body "Create_body"
    And add to JSON body "Create_body" from context
      | name        | name2          |
      | age         | json_age       |
      | active      | json_active    |
      | rating      | json_rating    |
      | totalBooks  | json_totalBooks|
      | genres      | json_genres    |
      | scores      | json_scores    |
      | wealth      | json_wealth    |
      | followers   | json_followers |

    When send a POST request
    Then validate status code of 201
    And verify response schema "author_schema"
    And verify response json path "$.name" equals "${name2}"

    And extract values from response
      | $.id | json_author_id |

    # Test mixed data operations
    Given load test data from "testdata/authors.csv"
    And use test data row 1

    And set endpoint to "/authors/{csv_author_id}"
    And add to path parameters from context
      | csv_author_id | csv_author_id |
    And clear body "Create_body"
    And add to JSON body "Create_body" from context
      | name   | name3      |
      | age    | csv_age    |
      | active | csv_active |

    When send a PUT request
    Then validate status code of 200
    And verify response json path "$.name" equals "${name3}"

    # Test response contains verification
    And verify response contains "id"
    And verify response contains "${name3}"

    # Test schema validation from classpath
    And verify response schema from classpath "author_schema"

    # Test performance on multiple operations
    Then validate status code of 200
    And verify last request response time is less than 500 ms

    # Cleanup both authors
    And set endpoint to "/authors/{csv_author_id}"
    When send a DELETE request
    Then validate status code of 204

    And set endpoint to "/authors/{json_author_id}"
    When send a DELETE request
    Then validate status code of 204

  Scenario: Edge cases and comprehensive flow testing
    # Test edge cases with missing data
    Given load test data from "testdata/authors.json"
    And use test data row 4

    And set endpoint to "/authors"
    And generate random values
      | name1      | letters(10)        |

    And add to JSON body "Create_body" from context
      | name   | name1 |
      | age    | json_age  |
      | active | json_active |
      | rating | json_rating |

    When send a POST request
    Then validate status code of 201
    And verify response json path "$.name" equals "${name1}"
    And verify response json path "$.rating" equals null
    And verify response json path "$.rating" is null

    And extract values from response
      | $.id | edge_author_id |

    # Test partial update validation
    And set endpoint to "/authors/{edge_author_id}"
    And set JSON body "Create_body" with:
      | age | 126 |
    When send a PATCH request
    Then validate status code of 400
    And verify response json path "$.error" equals "Age must not exceed 125"

    # Test search endpoint
    And set endpoint to "/authors/search?name=NonExistent"
    When send a GET request
    Then validate status code of 400
    And verify response contains "error"
    And verify response json path "$.error" exists

    # Test multiple operations with performance tracking
    Given set endpoint to "/authors"
    And generate random values
      | multiName1 | letters(9) |
      | multiName2 | letters(10) |

    And add to JSON body "Create_body" from context
      | name   | multiName1    |
    And set JSON body "Create_body" with:
      | age    | 28            |
      | active | true          |
    When send a POST request
    Then validate status code of 201
    And verify last request response time is less than 500 ms
    And extract values from response
      | $.id | multi_author_1 |

    Given set endpoint to "/authors"
    And add to JSON body "Create_body" from context
      | name   | multiName2    |
    And set JSON body "Create_body" with:
      | age    | 32            |
      | active | false         |
    When send a POST request
    Then validate status code of 201
    And verify last request response time is less than 200 ms
    And extract values from response
      | $.id | multi_author_2 |

    # Cleanup all authors
    And set endpoint to "/authors/{edge_author_id}"
    When send a DELETE request
    Then validate status code of 204

    And set endpoint to "/authors/{multi_author_1}"
    When send a DELETE request
    Then validate status code of 204

    And set endpoint to "/authors/{multi_author_2}"
    When send a DELETE request
    Then validate status code of 204