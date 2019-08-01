import * as React from "react";
import {withStyles} from '@material-ui/core';
import profileDialogStyles from './profileDialogStyles'
import connectService from "../../common/connectService";
import Dialog from "../common/Dialog/Dialog";
import DialogTitle from "@material-ui/core/DialogTitle";
import DialogContent from "@material-ui/core/DialogContent";
import TextField from "@material-ui/core/TextField";
import DialogActions from "@material-ui/core/DialogActions";
import Button from "@material-ui/core/Button";
import Tooltip from "@material-ui/core/Tooltip";
import IconButton from "@material-ui/core/IconButton";
import ProfileContext from "../../modules/profile/ProfileContext";
import {withSnackbar} from "notistack";

class ProfileDialog extends React.Component {
  textField = React.createRef();

  constructor(props) {
    super(props);
    this.state = {
      name: null,
      loading: false
    }
  }

  copyToClipboard = () => {
    navigator.clipboard.writeText(this.textField.current.props.value);
  };

  onChangeName = (event) => {
    this.setState({
      name: event.target.value
    });
  };

  onSubmit = (event) => {
    event.preventDefault();
    this.setState({
      loading: true
    });

    return this.props.setProfileField('name', this.state.name)
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
    const {classes} = this.props;
    return (
      <Dialog
        open={this.props.open}
        className={classes.dialog}
        onClose={this.props.onClose}
        loading={this.state.loading}
        aria-labelledby="form-dialog-title"
      >
        <form onSubmit={this.onSubmit}>
          <DialogTitle
            id="customized-dialog-title"
            onClose={this.props.onClose}
          >
            My Profile
          </DialogTitle>
          <DialogContent>
            <TextField
              className={classes.textField}
              defaultValue={this.state.name === null ? this.props.profile.name : this.state.name}
              disabled={this.state.loading}
              margin="normal"
              label="Name"
              type="text"
              fullWidth
              onChange={this.onChangeName}
              variant="outlined"
            />
            <TextField
              className={classes.textField}
              value={this.props.publicKey}
              label="Public Key"
              autoFocus
              disabled={true}
              margin="normal"
              fullWidth
              ref={this.textField}
              type="text"
              variant="outlined"
              InputProps={{
                classes: { input: classes.input },
                endAdornment: (
                  <IconButton
                    className={classes.iconButton}
                    onClick={this.copyToClipboard}
                    disabled={this.state.loading}
                  >
                    <Tooltip title="Copy" aria-label="Copy">
                      <i className="material-icons">
                        file_copy
                      </i>
                    </Tooltip>
                  </IconButton>
                ),
              }}
            >
            </TextField>
          </DialogContent>
          <DialogActions>
            <Button
              className={this.props.classes.saveButton}
              color="primary"
              variant="contained"
              type="submit"
              disabled={this.state.loading}
            >
              Save
            </Button>
          </DialogActions>
        </form>
      </Dialog>
    )
  }
}

export default connectService(
  ProfileContext,
  ({profile}, profileService) => ({
    profile,
    setProfileField(fieldName, value) {
      return profileService.setProfileField(fieldName, value);
    }
  })
)(
  withSnackbar(withStyles(profileDialogStyles)(ProfileDialog))
);
