@rest
Feature: Sample REST API Tests
  As a QA engineer
  I want to test REST APIs using Cucumber
  So that I can validate API behavior in a BDD style

  Background:
    Given the REST API base URL is "https://jsonplaceholder.typicode.com"

  # -------------------------------------------------------
  # GET requests
  # -------------------------------------------------------

  Scenario: GET a single user by ID
    When I send a GET request to "/users/1"
    Then the response status code should be 200
    And the response body should be valid JSON
    And the JSON path "$.name" should not be empty
    And the JSON path "$.id" should equal 1
    And the response time should be less than 10000 ms

  Scenario: GET all posts
    When I send a GET request to "/posts"
    Then the response status code should be 200
    And the response content type should be "application/json"
    And the JSON path "$" should not be empty

  Scenario: GET posts with query parameter
    Given I set query parameter "userId" to "1"
    When I send a GET request to "/posts"
    Then the response status code should be 200
    And the JSON path "$[0].userId" should equal 1

  # -------------------------------------------------------
  # POST requests
  # -------------------------------------------------------

  Scenario: POST to create a new post
    Given I set header "Content-Type" to "application/json"
    When I send a POST request to "/posts" with body:
      """
      {
        "title": "Test Post",
        "body": "This is a test post body",
        "userId": 1
      }
      """
    Then the response status code should be 201
    And the response body should be valid JSON
    And the JSON path "$.title" should equal "Test Post"
    And the JSON path "$.id" should not be empty

  # -------------------------------------------------------
  # PUT requests
  # -------------------------------------------------------

  Scenario: PUT to update an existing post
    When I send a PUT request to "/posts/1" with body:
      """
      {
        "id": 1,
        "title": "Updated Title",
        "body": "Updated body content",
        "userId": 1
      }
      """
    Then the response status code should be 200
    And the JSON path "$.title" should equal "Updated Title"

  # -------------------------------------------------------
  # DELETE requests
  # -------------------------------------------------------

  Scenario: DELETE an existing post
    When I send a DELETE request to "/posts/1"
    Then the response status code should be 200

  # -------------------------------------------------------
  # Chained requests (store and reuse data)
  # -------------------------------------------------------

  Scenario: Create a post and verify it
    When I send a POST request to "/posts" with body:
      """
      {
        "title": "Chained Test",
        "body": "Testing data extraction",
        "userId": 1
      }
      """
    Then the response status code should be 201
    And I store the JSON path "$.id" as "newPostId"
    And I print the response body

  # -------------------------------------------------------
  # Headers and authentication (example structure)
  # -------------------------------------------------------

  Scenario: Send request with custom headers
    Given I set the following headers:
      | Accept       | application/json |
      | X-Request-Id | test-12345       |
    When I send a GET request to "/users/1"
    Then the response status code should be 200
    And the response body should contain "Leanne Graham"
