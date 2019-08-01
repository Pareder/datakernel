import React from 'react';
import IconButton from '@material-ui/core/IconButton';
import Menu from '@material-ui/core/Menu';
import MenuItem from '@material-ui/core/MenuItem';
import MoreVertIcon from '@material-ui/icons/MoreVert';
import simpleMenuStyles from "./simpleMenuStyles";
import {withStyles} from '@material-ui/core';

function SimpleMenu({onDeleteDocument, onRenameDocument, classes}) {
  const [anchorEl, setAnchorEl] = React.useState(null);
  const open = Boolean(anchorEl);
  const options = {
    'Rename document': handleRename,
    'Delete document': handleDelete
  };

  function handleClick(event) {
    if (open) {
      setAnchorEl(null);
    } else {
      setAnchorEl(event.currentTarget);
    }
  }

  function handleClose() {
    setAnchorEl(null);
  }

  function handleDelete() {
    onDeleteDocument();
    handleClose();
  }

  function handleRename() {
    onRenameDocument();
    handleClose();
  }

  return (
    <div onClick={handleClick} className={classes.wrapperButton}>
      <IconButton
        aria-label="More"
        aria-controls="simple-menu"
        aria-haspopup="true"
        className={classes.iconButton}
      >
        <MoreVertIcon/>
      </IconButton>
      <Menu
        id="simple-menu"
        anchorEl={anchorEl}
        keepMounted
        open={open}
        onClose={handleClose}
        PaperProps={{
          style: {
            maxHeight: 48 * 4.5,
            width: 200,
          },
        }}
      >
        {Object.entries(options).map(([option, action]) => (
          <MenuItem
            key={option}
            onClick={action}
          >
            {option}
          </MenuItem>
        ))}
      </Menu>
    </div>
  );
}

export default withStyles(simpleMenuStyles)(SimpleMenu);
