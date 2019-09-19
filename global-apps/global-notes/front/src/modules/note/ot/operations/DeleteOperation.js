import InsertOperation from './InsertOperation';

class DeleteOperation {
  constructor(position, content) {
    this.position = position;
    this.content = content;
  }

  apply(state) {
    return (
      state.substr(0, this.position) + state.substr(this.position + this.content.length)
    );
  }

  invert() {
    return new InsertOperation(this.position, this.content);
  }

  isEqual(deleteOperation) {
    return deleteOperation.position === this.position && deleteOperation.content === this.content;
  }

  isEmpty() {
    return this.content.length === 0;
  }
}

export default DeleteOperation;
