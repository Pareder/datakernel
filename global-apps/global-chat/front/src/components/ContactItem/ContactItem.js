import React from "react";
import path from 'path'
import {withStyles} from '@material-ui/core';
import ListItemAvatar from "@material-ui/core/ListItemAvatar";
import Avatar from "@material-ui/core/Avatar";
import ListItemText from "@material-ui/core/ListItemText";
import ListItem from "@material-ui/core/ListItem";
import contactItemStyles from "./contactItemStyles";
import {withRouter} from "react-router-dom";
import {getAvatarLetters, getDialogRoomId} from "../../common/utils";
import ConfirmDialog from "../ConfirmDialog/ConfirmDialog";

class ContactItem extends React.Component {
  state = {
    hover: false, // TODO
    showAddContactDialog: false
  };

  getAvatarLetters = contact => {
    if (contact.firstName !== null && contact.lastName !== null) {
      return getAvatarLetters(contact.firstName + ' ' + contact.lastName).toUpperCase();
    }
    return getAvatarLetters(contact.username).toUpperCase();
  };

  onContactClick() {
    this.setState({
      showAddContactDialog: true
    });
  }

  onClickAddContact = () => {
    this.props.onAddContact(this.props.contactId, this.getContactName())
      .then(() => {
        this.onCloseAddContactDialog();
      })
      .catch((err) => {
        this.props.enqueueSnackbar(err.message, {
          variant: 'error'
        });
      })
  };

  onCloseAddContactDialog = () => {
    this.setState({
      hover: false,
      showAddContactDialog: false
    });
  };

  getContactName = () => {
    return this.props.contact.firstName !== null && this.props.contact.lastName !== null ?
      this.props.contact.firstName + ' ' + this.props.contact.lastName :
      this.props.contact.username
  };

  toggleHover = () => {
    this.setState({hover: !this.state.hover})
  };

  render() {
    const {classes, contactId, contact} = this.props;
    return (
      <>
        <ListItem
          onClick={this.onContactClick.bind(this)}
          onMouseEnter={this.toggleHover}
          onMouseLeave={this.toggleHover}
          className={classes.listItem}
          button
          selected={getDialogRoomId([this.props.publicKey, contactId]) === this.props.match.params.roomId}
        >
          <ListItemAvatar className={classes.avatar}>
            <Avatar className={classes.avatarContent}>
              {this.getAvatarLetters(contact)}
            </Avatar>
          </ListItemAvatar>

          <ListItemText
            primary={this.getContactName()}
            className={classes.itemText}
            classes={{
              primary: classes.itemTextPrimary
            }}
          />
        </ListItem>
        <ConfirmDialog
          open={this.state.showAddContactDialog}
          onClose={this.onCloseAddContactDialog}
          title="Add Contact"
          subtitle="Do you want to add this contact?"
          onConfirm={this.onClickAddContact}
        />
      </>
    )
  }
}

export default withRouter(withStyles(contactItemStyles)(ContactItem));


