import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';
import {Client, Status, LatLng, TravelMode} from "@googlemaps/google-maps-services-js";
import * as util from "@googlemaps/google-maps-services-js/dist/util";

// // Start writing Firebase Functions
// // https://firebase.google.com/docs/functions/typescript
//

admin.initializeApp();
const db = admin.firestore();


export const helloWorld = functions.https.onRequest((request, response) => {
    response.send("Hello from Firebase!");
    console.log('Please work');
});

export const findMidpoint = functions.https.onCall((data) => {

    console.log(data.docID);

    let origin:LatLng = '';
    let dest:LatLng = '';

    let reqRef = db.collection('Requests').doc(data.docID);
    reqRef.get()
        .then(doc => {
        if (!doc.exists) {
            console.log('No such document!');
        } else {
            if (!doc.data()){
                console.log("Undefined data");
            }
            console.log(doc.data()!.primaryUserLocation);
            origin = doc.data()!.primaryUserLocation;
            console.log(doc.data()!.secondaryUserLocation);
            dest = doc.data()!.secondaryUserLocation;
        }

        const client = new Client({});
    client.directions({
        params:{
            origin: origin,
            destination: dest,
            mode: TravelMode["driving"],
            key: "AIzaSyACLyHMHhi7tsD7JRHAD4zubgFVZ7TepQQ"
        },
        timeout: 100000, //milliseconds
    })
    .then((r) => {
        if (r.data.status === Status.OK) {
          const decodedPath = util.decodePath(r.data.routes[0].overview_polyline.points);
          const midIndex = Math.floor((decodedPath.length)/2);
          const mPoint = decodedPath[midIndex].lat.toString() +  "," + decodedPath[midIndex].lng.toString();
          console.log("\nMidpoint: " + mPoint);
          console.log("Amount of routes: " + r.data.routes.length + "\nStatus: " + r.data.status + "\nMidIndex: " + midIndex);

          db.collection("Requests").doc(data.docID).update({
            midpoint: mPoint
          })
            .then(function() {
                console.log("Document successfully written!");
            })
            .catch(function(error) {
                console.error("Error writing document: ", error);
            });

        } else {
          console.log(r.data.error_message);
          console.log("Status" + r.data.status.toString());
        }
      })
      .catch((e) => {
        console.log(e);
        console.log("Something went wrong :(");
    });
        
    })
    .catch(err => {
        console.log('Error getting document', err);
    });

    
});