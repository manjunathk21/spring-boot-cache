# assortment-truth-service  

This service receives the payload from MAC. As part of this, following actions are performed
 * calls assortment-truth-aggregator API to create the request.
 * sends the payload to assortment-truth-validation API for validation.
 
## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes. 
 
### checkout project using below github repository

 ```
  https://github.homedepot.com/assortment-commons/assortment-truth-service.git
 ```
 
### pull all dependencies using below command 
  
 ```
  ./gradlew clean build 
 ```
 
### set up below environments inside IDE 

vcap.services.sku-services.credentials.url=https://webapps-qa.homedepot.com/MsrSkuWSRR/rs/reasonCodes?application=2

vcap.services.assortment-truth.credentials.assortment-truth-validation-url=https://assortment-truth-validation-development.apps-np.homedepot.com

vcap.services.assortment-truth.credentials.aggregator-url=https://assortment-truth-aggregator-development.apps-np.homedepot.com/assortment/aggregator

vcap.services.assortment-truth.credentials.slack-channel-id=CKL7SSYUR

vcap.services.assortment-truth.credentials.slack-token=xoxp-3793049082-18869819268-661352331667-ec760480935300e3d20b6507f354d8f9

vcap.services.assortment-truth-service.credentials.splunk-search-index=qa_app_logs

vcap.services.assortment-truth-service.credentials.splunk-search-service=cloudfoundry/assortment-commons/development

vcap.application.name=assortment-truth-service

vcap.application.space_name=development

vcap.services.assortment-truth.credentials.security-db-username=***************

vcap.services.assortment-truth.credentials.security-db-password=****************

vcap.services.assortment-truth.credentials.security-db-jdbc-url=jdbc:mysql://MariaDBQASE3.homedepot.com:3306/qamdb30592

vcap.services.assortment-truth.credentials.security-db-driver=com.mysql.jdbc.Driver

vcap.services.assortment-truth.credentials.hmac-refresh-window=5000

vcap.services.assortment-truth.credentials.secret-key=****************

vcap.services.assortment-truth.credentials.http-connection-pool-max-size=20

vcap.services.assortment-commons-services.credentials.url=https://assortment-commons-services.hd-assrtmgmt-dev.gcp.homedepot.com/assortments/skus/locations

vcap.services.assortment-commons-services.credentials.client-key=*************

vcap.services.assortment-commons-services.credentials.client-secret=************

vcap.services.assortment-commons-services.credentials.payload-size=25

vcap.services.assortment-validation.credentials.mcafee-token=**********

vcap.services.thd-proxy.credentials.host=thd-svr-proxy-qa.homedepot.com

vcap.services.thd-proxy.credentials.port=9191

vcap.services.assortment-truth.credentials.admin-url=https://assortment-truth-admin-acc.apps-np.homedepot.com

version=v0.0.0

### Run spring boot application inside IDE 

Right Click AssortmentTruthServiceApplication and click 'Run AssortmentTruthServiceApplication'

### Running the tests

```
 ./gradlew clean build jacocoTestReport
```

 Note: if code coverage below 80 percent, build will be failed during execution  
 
### Jenkins CI/CD pipeline

```
http://lnc4e05.homedepot.com/job/Assortment-Commons-PCF/job/assortment-truth-service
```
