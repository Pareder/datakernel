import React from "react";
import {withStyles} from '@material-ui/core';
import Button from '@material-ui/core/Button';
import Dialog from '../Dialog/Dialog';
import DialogActions from '@material-ui/core/DialogActions';
import DialogContent from '@material-ui/core/DialogContent';
import DialogContentText from '@material-ui/core/DialogContentText';
import DialogTitle from '@material-ui/core/DialogTitle';
import TextField from '@material-ui/core/TextField';
import { withSnackbar } from 'notistack';
import addContactDialogStyles from "./addContactDialogStyles";

class AddContactDialog extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      pubKey: props.contactPublicKey || '',
      name: '',
      loading: false,
    };
  }

  handlePKChange = (event) => {
    this.setState({pubKey: event.target.value});
  };

  handleNameChange = (event) => {
    this.setState({name: event.target.value});
  };

  onSubmit = (event) => {
    event.preventDefault();

    this.setState({
      loading: true
    });

    if (this.props.contactPublicKey === this.props.publicKey) {
      this.setState({
        loading: false
      });
      this.props.enqueueSnackbar('Can\'t add yourself', {
        variant: 'error'
      });
      this.props.onClose();
      return;
    }

    return this.props.addContact(this.props.contactPublicKey, this.state.name)
      .then(() => {
        this.props.onClose();
      })
      .catch((err) => {
        this.props.enqueueSnackbar(err.message, {
          variant: 'error'
        });
      })
      .finally(() => {
        this.setState({
          loading: false
        });
      });
  };

  render() {
    return (
      <Dialog
        open={this.props.open}
        onClose={this.props.onClose}
        loading={this.state.loading}
        aria-labelledby="form-dialog-title"
      >
        <form onSubmit={this.onSubmit}>
          <DialogTitle id="customized-dialog-title" onClose={this.props.onClose}>Add Contact</DialogTitle>
          <DialogContent>
            <DialogContentText>
              Enter contact name to start chat
            </DialogContentText>
            <TextField
              required={true}
              className={this.props.classes.textField}
              autoFocus
              disabled={this.state.loading}
              margin="normal"
              label="Name"
              type="text"
              fullWidth
              variant="outlined"
              onChange={this.handleNameChange}
            />
            {!this.props.contactPublicKey && (
              <TextField
                required={true}
                className={this.props.classes.textField}
                disabled={this.state.loading}
                margin="normal"
                label="Key"
                defaultValue={this.props.contactPublicKey || ''}
                type="text"
                fullWidth
                variant="outlined"
                onChange={this.handlePKChange}
              />
            )}
          </DialogContent>
          <DialogActions>
            <Button
              className={this.props.classes.actionButton}
              disabled={this.state.loading}
              onClick={this.props.onClose}
            >
              Cancel
            </Button>
            <Button
              className={this.props.classes.actionButton}
              loading={this.state.loading}
              type={"submit"}
              color={"primary"}
              variant={"contained"}
            >
              Add Contact
            </Button>
          </DialogActions>
        </form>
      </Dialog>
    );
  }
}

export default withSnackbar(withStyles(addContactDialogStyles)(AddContactDialog));
