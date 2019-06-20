import React from "react";
import connectService from "../common/connectService";
import AccountContext from "../modules/account/AccountContext";
import qs from 'query-string';
import Redirect from "react-router-dom/es/Redirect";

class ComeBack extends React.Component {
  render() {
    let params = qs.parse(this.props.location.search);
    let privateKey = params.privateKey;

    if (privateKey){
      this.props.accountService.authByKey(privateKey);
    }

    return <Redirect to="/"/>;
  }

}

export default connectService(AccountContext, (state, accountService) => ({accountService}))(ComeBack);

