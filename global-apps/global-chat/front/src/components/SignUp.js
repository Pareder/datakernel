import React from "react";
import connectService from "../common/connectService";
import AccountContext from "../modules/account/AccountContext";

const APP_STORE_URL = 'http://localhost:9999';
const EXT_AUTH_HREF = APP_STORE_URL + '/extAuth?redirectUri=' + window.location.origin + '/comeback';

class SignUp extends React.Component {
  constructor(props) {
    super(props);

    this.state = {privKey: ''};

    this._handleChange = this._handleChange.bind(this);
    this._handleSubmit = this._handleSubmit.bind(this);
  }

  _handleSubmit(e) {
    e.preventDefault();
    this.props.accountService.authByKey(this.state.privKey);
    this.props.history.push('/');
  }

  _handleChange(e) {
    this.setState({privKey: e.target.value})
  }

  render() {
    return <div>
      <form onSubmit={this._handleSubmit}>
        <label>
          Private Key:
          <input type="text" name="name" onChange={this._handleChange}/>
        </label>
        <input type="submit" value="Submit"/>
      </form>
      <a href={EXT_AUTH_HREF}>
        Log in via App Store
      </a>
    </div>
  }

}

export default connectService(AccountContext, (state, accountService) => ({accountService}))(SignUp);

