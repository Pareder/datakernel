import React from 'react';
import {BrowserRouter as Router, Route, Switch} from 'react-router-dom';
import MainScreen from "./MainScreen/MainScreen";
import SignUp from "./SignUp/SignUp";
import {withSnackbar} from 'notistack';
import AccountCallback from "./AccountCallback/AccountCallback";

function App() {
  return (
    <Router>
      <Switch>
        <Route exact={true} path="/sign-up" component={SignUp}/>
        <Route path="/sign-up/auth" component={AccountCallback}/>
        <Route path="/room/:roomId" component={MainScreen}/>
        <Route path="/" component={MainScreen}/>
      </Switch>
    </Router>
  );
}

export default withSnackbar(App);
