# Credit card service

## How to run

**Requirements**
- Java 21
- Docker

**How to build**  
This will compile, run tests and produce a fat jar.
```bash
./gradlew clean build
```

In order to just run the tests simply run
```bash
./gradlew clean test
```


### How to run the service locally

#### 1. Start the service dependencies  
```bash 
make local-up 
```
This will start the following services as docker containers:   
- Postgres
  
 
#### 2. Start the server
```bash
make local-run
```
This runs the server application configured to talk to the docker based dependencies


#### 3. Make requests

**POST /credit-cards**:
```bash
curl --request POST \
  --url http://localhost:8080/credit-cards \
  --header 'Content-Type: application/json' \
  --data '{
  "name": "foo",
  "number": "5234567891112135",
  "expiry": "11/26",
  "limit": 0
}'
```
**Note:** a limit of 0 is treated as "unlimited"
  

**GET /credit-cards**:
```bash
curl --request GET \
  --url http://localhost:8080/credit-cards \
  --header 'Content-Type: application/json'
```

**GET /credit-cards/{id}**:
```bash
curl --request GET \
  --url http://localhost:8080/credit-cards/5234567891112135 \
  --header 'Content-Type: application/json'
```

**PUT /credit-cards/{id}**:
```bash
curl --request PUT \
  --url http://localhost:8080/credit-cards/5234567891112135 \
  --header 'Content-Type: application/json' \
  --data '{
  "limit": 500
}'
```

**DELETE /credit-cards/{id}**:
```bash
curl --request DELETE \
  --url http://localhost:8080/credit-cards/5234567891112135 \
  --header 'Content-Type: application/json'
```

**POST /credit-cards/{id}/charge**:
```bash
curl --request POST \
  --url http://localhost:8080/credit-cards/5234567891112135/charge \
  --header 'Content-Type: application/json' \
  --data '{
  "amount": 512,
  "shopId": "66669b22-d6ea-4847-a82a-907c410b9a35"
}'
```

**POST /credit-cards/{id}/credit**:
```bash
curl --request POST \
  --url http://localhost:8080/credit-cards/5234567891112135/credit \
  --header 'Content-Type: application/json' \
  --data '{
  "amount": 512,
  "shopId": "66669b22-d6ea-4847-a82a-907c410b9a35"
}'
```

**Making requests using Bruno**  
Want some postman like UI to make API requests, try [Bruno](https://www.usebruno.com/)!  

1. Download and install Bruno
2. Import the collection in `Bruno/bruno.js`


## Thoughts
Following are a couple of assumptions and thoughts:   
In a real world scenario I would be concerned about the use of card numbers as identifier for APIs.  

The card number is a sensitive piece of information and must be stored securely and at the same time we're using that same piece of sensitive information liberally as identifier for requests. Which means the card number must be circulating somewhere else in the system which it probably shouldn't.  

My initial instinct would be to hide the card details behind some kind of token, a unique id that represents this card and can be passed around freely and only when necessary for example when charging the card, etc would that token be resolved into the full card details.

Credit cards records are created as-is without any de-duplication.   
Depending on the product, there might be a user or tenant account those credit cards are attached to which define uniqueness constraints. 

The service does not have any concept of RBAC (authn, authz).   
The assumption is that an API gateway or the likes does the auth and establishes a session.   
Downstream requests would contain some kind of token (JWT, session token, etc) in the header which defines the context (user/tenant/transaction).  

This service does not deal with currencies.  
In future iterations storing the primary currency of a card might be a good idea, as well as adding the currency a credit card transaction is supposed to happen in. 