import * as functions from 'firebase-functions';

// // Start writing Firebase Functions
// // https://firebase.google.com/docs/functions/typescript
//
export const helloWorld = functions.https.onRequest((request, response) => {
    console.log("Hello World! Testing Firebase cloud functions!")
    response.send("Hello from Firebase!");
});

export const findMidpoint = functions.https.onRequest((request, response) => {
    //directions api call

    //parse json response

    //java midpoint function using LatLng list
});
