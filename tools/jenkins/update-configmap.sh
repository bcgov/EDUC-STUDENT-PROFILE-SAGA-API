envValue=$1
APP_NAME=$2
PEN_NAMESPACE=$3
COMMON_NAMESPACE=$4
APP_NAME_UPPER=${APP_NAME^^}
TZVALUE="America/Vancouver"
SOAM_KC_REALM_ID="master"
KCADM_FILE_BIN_FOLDER="/tmp/keycloak-9.0.3/bin"
SOAM_KC=soam-$envValue.apps.silver.devops.gov.bc.ca
NATS_CLUSTER=educ_nats_cluster
NATS_URL="nats://nats.${COMMON_NAMESPACE}-${envValue}.svc.cluster.local:4222"

oc project $COMMON_NAMESPACE-$envValue
SOAM_KC_LOAD_USER_ADMIN=$(oc -o json get secret sso-admin-${envValue} | sed -n 's/.*"username": "\(.*\)"/\1/p' | base64 --decode)
SOAM_KC_LOAD_USER_PASS=$(oc -o json get secret sso-admin-${envValue} | sed -n 's/.*"password": "\(.*\)",/\1/p' | base64 --decode)

oc project $PEN_NAMESPACE-$envValue
SPLUNK_TOKEN=$(oc -o json get configmaps "${APP_NAME}"-"${envValue}"-setup-config | sed -n "s/.*\"SPLUNK_TOKEN_${APP_NAME_UPPER}\": \"\(.*\)\"/\1/p")

oc project $PEN_NAMESPACE-tools

###########################################################
#Fetch the public key
###########################################################
"$KCADM_FILE_BIN_FOLDER"/kcadm.sh config credentials --server https://"$SOAM_KC"/auth --realm $SOAM_KC_REALM_ID --user "$SOAM_KC_LOAD_USER_ADMIN" --password "$SOAM_KC_LOAD_USER_PASS"
getPublicKey() {
  executorID= "$KCADM_FILE_BIN_FOLDER"/kcadm.sh get keys -r $SOAM_KC_REALM_ID | grep -Po 'publicKey" : "\K([^"]*)'
}

echo Fetching public key from SOAM
soamFullPublicKey="-----BEGIN PUBLIC KEY----- $(getPublicKey) -----END PUBLIC KEY-----"

###########################################################
#Setup for scopes
###########################################################

#STUDENT_PROFILE_COMPLETE_SAGA
"$KCADM_FILE_BIN_FOLDER"/kcadm.sh create client-scopes -r $SOAM_KC_REALM_ID --body "{\"description\": \"Access to execute complete Saga for Student profile.\",\"id\": \"STUDENT_PROFILE_COMPLETE_SAGA\",\"name\": \"STUDENT_PROFILE_COMPLETE_SAGA\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"

#STUDENT_PROFILE_COMMENT_SAGA
"$KCADM_FILE_BIN_FOLDER"/kcadm.sh create client-scopes -r $SOAM_KC_REALM_ID --body "{\"description\": \"Access to add comment and update status for student profile\",\"id\": \"STUDENT_PROFILE_COMMENT_SAGA\",\"name\": \"STUDENT_PROFILE_COMMENT_SAGA\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"

#STUDENT_PROFILE_REJECT_SAGA
"$KCADM_FILE_BIN_FOLDER"/kcadm.sh create client-scopes -r $SOAM_KC_REALM_ID --body "{\"description\": \"Access to Reject a student profile request\",\"id\": \"STUDENT_PROFILE_REJECT_SAGA\",\"name\": \"STUDENT_PROFILE_REJECT_SAGA\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"

#STUDENT_PROFILE_RETURN_SAGA
"$KCADM_FILE_BIN_FOLDER"/kcadm.sh create client-scopes -r $SOAM_KC_REALM_ID --body "{\"description\": \"Access to Return a student profile request\",\"id\": \"STUDENT_PROFILE_RETURN_SAGA\",\"name\": \"STUDENT_PROFILE_RETURN_SAGA\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"

#PEN_REQUEST_COMPLETE_SAGA
"$KCADM_FILE_BIN_FOLDER"/kcadm.sh create client-scopes -r $SOAM_KC_REALM_ID --body "{\"description\": \"Scope for completing a PEN request\",\"id\": \"PEN_REQUEST_COMPLETE_SAGA\",\"name\": \"PEN_REQUEST_COMPLETE_SAGA\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"

#PEN_REQUEST_COMMENT_SAGA
"$KCADM_FILE_BIN_FOLDER"/kcadm.sh create client-scopes -r $SOAM_KC_REALM_ID --body "{\"description\": \"Scope for adding PEN request comments and updating its status\",\"id\": \"PEN_REQUEST_COMMENT_SAGA\",\"name\": \"PEN_REQUEST_COMMENT_SAGA\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"

#PEN_REQUEST_RETURN_SAGA
"$KCADM_FILE_BIN_FOLDER"/kcadm.sh create client-scopes -r $SOAM_KC_REALM_ID --body "{\"description\": \"Scope to return a PEN request for more info\",\"id\": \"PEN_REQUEST_RETURN_SAGA\",\"name\": \"PEN_REQUEST_RETURN_SAGA\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"

#PEN_REQUEST_REJECT_SAGA
"$KCADM_FILE_BIN_FOLDER"/kcadm.sh create client-scopes -r $SOAM_KC_REALM_ID --body "{\"description\": \"Scope to reject a PEN request \",\"id\": \"PEN_REQUEST_REJECT_SAGA\",\"name\": \"PEN_REQUEST_REJECT_SAGA\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"

#PEN_REQUEST_UNLINK_SAGA
"$KCADM_FILE_BIN_FOLDER"/kcadm.sh create client-scopes -r $SOAM_KC_REALM_ID --body "{\"description\": \"Scope to unlink a PEN request after it is completed\",\"id\": \"PEN_REQUEST_UNLINK_SAGA\",\"name\": \"PEN_REQUEST_UNLINK_SAGA\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"

#READ_SAGA
"$KCADM_FILE_BIN_FOLDER"/kcadm.sh create client-scopes -r $SOAM_KC_REALM_ID --body "{\"description\": \"Scope to READ a SAGA record by its ID\",\"id\": \"STUDENT_PROFILE_READ_SAGA\",\"name\": \"STUDENT_PROFILE_READ_SAGA\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"
###########################################################
#Setup for config-map
###########################################################
###########################################################
#Setup for config-map
###########################################################
SPLUNK_URL="gww.splunk.educ.gov.bc.ca"
FLB_CONFIG="[SERVICE]
   Flush        1
   Daemon       Off
   Log_Level    debug
   HTTP_Server   On
   HTTP_Listen   0.0.0.0
   HTTP_Port     2020
   Parsers_File parsers.conf
[INPUT]
   Name   tail
   Path   /mnt/log/*
   Exclude_Path *.gz,*.zip
   Parser docker
   Mem_Buf_Limit 20MB
[FILTER]
   Name record_modifier
   Match *
   Record hostname \${HOSTNAME}
[OUTPUT]
   Name   stdout
   Match  *
[OUTPUT]
   Name  splunk
   Match *
   Host  $SPLUNK_URL
   Port  443
   TLS         On
   TLS.Verify  Off
   Message_Key $APP_NAME
   Splunk_Token $SPLUNK_TOKEN
"
PARSER_CONFIG="
[PARSER]
    Name        docker
    Format      json
"
echo
echo Creating config map "$APP_NAME"-config-map
oc create -n "$PEN_NAMESPACE"-"$envValue" configmap "$APP_NAME"-config-map --from-literal=TZ=$TZVALUE --from-literal=NATS_URL="$NATS_URL" --from-literal=NATS_CLUSTER="$NATS_CLUSTER" --from-literal=KEYCLOAK_PUBLIC_KEY="$soamFullPublicKey" --from-literal=SPRING_SECURITY_LOG_LEVEL=INFO --from-literal=SPRING_WEB_LOG_LEVEL=INFO --from-literal=APP_LOG_LEVEL=INFO --from-literal=SPRING_BOOT_AUTOCONFIG_LOG_LEVEL=INFO --from-literal=SPRING_SHOW_REQUEST_DETAILS=false --from-literal=PURGE_RECORDS_SAGA_AFTER_DAYS=365 --from-literal=SCHEDULED_JOBS_PURGE_OLD_SAGA_RECORDS_CRON="@midnight" --from-literal=NATS_MAX_RECONNECT=60 --from-literal=TOKEN_ISSUER_URL="https://$SOAM_KC/auth/realms/$SOAM_KC_REALM_ID" --dry-run -o yaml | oc apply -f -
echo
echo Setting environment variables for "$APP_NAME"-$SOAM_KC_REALM_ID application
oc project "$PEN_NAMESPACE"-"$envValue"
oc set env --from=configmap/"$APP_NAME"-config-map dc/"$APP_NAME"-$SOAM_KC_REALM_ID

echo Creating config map "$APP_NAME"-flb-sc-config-map
oc create -n "$PEN_NAMESPACE"-"$envValue" configmap "$APP_NAME"-flb-sc-config-map --from-literal=fluent-bit.conf="$FLB_CONFIG" --from-literal=parsers.conf="$PARSER_CONFIG" --dry-run -o yaml | oc apply -f -
