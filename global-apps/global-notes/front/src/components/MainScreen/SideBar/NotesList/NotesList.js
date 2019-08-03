import React from 'react';
import path from 'path';
import {withStyles} from '@material-ui/core';
import List from '@material-ui/core/List';
import CircularProgress from '@material-ui/core/CircularProgress';
import Grow from '@material-ui/core/Grow';
import NoteItem from './NoteItem/NoteItem';
import notesListStyles from './notesListStyles';

function NotesList({classes, ready, notes, onRename, onDelete}) {
  const getNotePath = noteId => {
    return path.join('/note', noteId || '');
  };

  return (
    <>
      {!ready && (
        <Grow in={!ready}>
          <div className={classes.progressWrapper}>
            <CircularProgress/>
          </div>
        </Grow>
      )}
      {ready && (
        <List>
          {Object.entries(notes).map(([noteId, noteName], index) =>
            (
              <NoteItem
                key={index}
                noteId={noteId}
                noteName={noteName}
                getNotePath={getNotePath}
                showMenuIcon={true}
                onRename={onRename}
                onDelete={onDelete}
              />
            )
          )}
        </List>
      )}
    </>
  );
}

export default withStyles(notesListStyles)(NotesList);
