import React from 'react';
import {withStyles} from '@material-ui/core';
import messagesStyles from './messagesStyles';
import MessageItem from "../MessageItem/MessageItem"
import CircularProgress from '@material-ui/core/CircularProgress';
import Grow from '@material-ui/core/Grow';
import ChatRoomContext from '../../modules/chatroom/ChatRoomContext';
import connectService from '../../common/connectService';
import AccountContext from "../../modules/account/AccountContext";
import RoomsContext from "../../modules/rooms/RoomsContext";
import ContactsContext from "../../modules/contacts/ContactsContext";
import {toEmoji} from "../../common/utils";

class Messages extends React.Component {
  wrapper = React.createRef();

  componentDidUpdate(prevProps) {
    if (
      this.wrapper.current
      && this.props.messages.length !== prevProps.messages.length
    ) {
      this.wrapper.current.scrollTop = this.wrapper.current.scrollHeight;
    }
  }

  render() {
    const {classes, chatReady, messages, publicKey} = this.props;
    return (
      <div className={classes.root}>
        {!chatReady && (
          <Grow in={!chatReady}>
            <div className={classes.progressWrapper}>
              <CircularProgress/>
            </div>
          </Grow>
        )}
        {chatReady && (
          <div ref={this.wrapper} className={classes.wrapper}>
            {messages.map((message, index) => {
              const previousMessageAuthor = messages[index - 1] && messages[index - 1].authorPublicKey;
              let shape = 'start';
              if (previousMessageAuthor === message.authorPublicKey) {
                shape = 'medium';
              }
              return (
                <MessageItem
                  key={index}
                  text={message.content}
                  author={
                    message.authorPublicKey === publicKey ? '' :
                      this.props.names.get(publicKey)
                  }
                  time={new Date(message.timestamp).toLocaleString()}
                  loaded={message.loaded}
                  drawSide={(message.authorPublicKey === publicKey) ? 'left' : 'right'}
                  shape={shape}
                />
              )
            })}
          </div>
        )}
      </div>
    )
  }
}

export default withStyles(messagesStyles)(
  connectService(ContactsContext, (
    {names}, contactsService) => (
      {contactsService, names })
  )(
    connectService(RoomsContext, (
      {rooms}, roomsService) => (
        {rooms, roomsService})
    )(
      connectService(ChatRoomContext, (
        {messages, chatReady}) => (
          {messages, chatReady})
      )(
        connectService(AccountContext, (
          {publicKey}) => ({publicKey})
        )(
          Messages
        )
      )
    )
  )
);
