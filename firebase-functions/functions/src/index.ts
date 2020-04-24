import * as functions from 'firebase-functions';
<<<<<<< Updated upstream

// // Start writing Firebase Functions
// // https://firebase.google.com/docs/functions/typescript
//
=======
import { LatLng, GeocodedWaypoint } from '@google/maps';
// // Start writing Firebase Functions
// // https://firebase.google.com/docs/functions/typescript
//

>>>>>>> Stashed changes
export const helloWorld = functions.https.onRequest((request, response) => {
    console.log("Hello World! Testing Firebase cloud functions!")
    response.send("Hello from Firebase!");
});

<<<<<<< Updated upstream
export const findMidpoint = functions.https.onRequest((request, response) => {
    //directions api call
=======
function findMidpoint(origin:LatLng, dest:LatLng){
    var directionsService = new google.maps.DirectionsService()
    //var waypoints;

    directionsService.route(
      {
        origin: origin.toString(),
        destination: dest.toString(),
        travelMode: google.maps.TravelMode['DRIVING'],
      },
      function(response, status) {
        if (status === 'OK') {
          //waypoints = response.route;
          console.log(response.geocoded_waypoints.length);
          console.log(response.geocoded_waypoints);
        } else {
          window.alert('Directions request failed due to ' + status);
        }
      });

      //var midpoint = calcMidpoint(waypoints);
}
>>>>>>> Stashed changes

    //parse json response

<<<<<<< Updated upstream
    //java midpoint function using LatLng list
});
=======
  //initially add two points to both the origin and des to establish baseline
  originList.push(waypoints[0]);
  originList.push(waypoints[1]);
  waypoints.splice(0,2);

  destList.push(waypoints[waypoints.length-1]);
  destList.push(waypoints[waypoints.length-2]);
  waypoints.pop();
  waypoints.pop();

  //calculate starting distance
  //var startDist = googleMapsClient.geometry.spherical.computeDistanceBetween(new LatLng(originList[0].))
}

var testOrigin:LatLng = [44.47593, -73.21277];
var testDest:LatLng = [42.11759, -71.86467];
findMidpoint(testOrigin, testDest);
console.log("testing");
>>>>>>> Stashed changes
