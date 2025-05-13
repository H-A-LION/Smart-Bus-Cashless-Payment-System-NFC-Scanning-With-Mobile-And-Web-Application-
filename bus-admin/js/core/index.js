var admin = require("firebase-admin");

var serviceAccount = require("smartbus-cashless-payment-nfc-firebase-adminsdk-fbsvc-5c388872ef.json");

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  databaseURL: "https://smartbus-cashless-payment-nfc-default-rtdb.firebaseio.com"
});

const uid='some-uid';
const additionalClaims={
    premiumAccount : true

};
admin.auth().createCustomToken(uid,additionalClaims)
.then((customToken)=>{
    console.log(customToken);
}).catch((error)=>{
    console.log("Error  creating custom Token: ",error);
});
