import React from 'react';
import classNames from 'classnames'
import {withStyles} from '@material-ui/core';
import callButtonsStyles from './callButtonsStyles';
import Fab from '@material-ui/core/Fab';
import PhoneIcon from '@material-ui/icons/Phone';

function CallButtons({classes, showAccept, showClose, onAccept, onClose, btnPressed}) {
  return (
    <>
      {showAccept && (
        <Fab
          color="primary"
          aria-label="accept"
          className={classNames(classes.fab, classes.fabAnimation)}
          onClick={onAccept}
          disabled={btnPressed}
        >
          <PhoneIcon/>
        </Fab>
      )}
      {showClose && (
        <Fab
          color="secondary"
          aria-label="decline"
          className={classes.fab}
          onClick={onClose}
          disabled={btnPressed}
        >
          <PhoneIcon/>
        </Fab>
      )}
    </>
  )
}

export default withStyles(callButtonsStyles)(CallButtons);
