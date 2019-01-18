import React, { Component } from 'react';
import './App.css';
import Compound from './Compound';
import UUID from 'uuid-js';

class App extends Component {

  render() {
    console.log('UUID', UUID);
    const uuid = UUID.create();
    console.log('uuid', uuid);
    return (
      <div className="App">
        <Compound />
      </div>
    );
  }
}

export default App;
