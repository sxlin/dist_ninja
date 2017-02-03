#include "peer_command_runner.h"

#include <asio/io_service.hpp>
#include <asio/use_future.hpp>
#include <iostream>
#include <sstream>

std::atomic_int PeerCommandRunner::g_IdCounter;
PeerCommandRunner *PeerCommandRunner::g_PeerCommandRunner;

static const double REQUEST_TIMOUT(300.0);

std::string ReadUntilDelim(std::istream &response_stream, char delim)
{
  char curchar;
  string retme;
  // Read until delim
  while (response_stream.good() && response_stream.peek() != delim) {
    response_stream.get(curchar);
    retme += curchar;
  }
  response_stream.get(); // pop delim
  return retme;
}

bool Peer::Connect() {
  tcp::endpoint bg_endpoint(asio::ip::address::from_string(m_address), 3456);

  m_io_service = new asio::io_service();
  m_socket = new tcp::socket(*m_io_service);

  try {
    m_socket->connect(bg_endpoint);
    cout << "Connected to peer " <<  m_address << endl;
    available = true;
  }
  catch (...) {
    cout << "Could not connect to peer " << m_address << endl;
  }

  if (available) {
    ReadResponse();
    std::thread([&]{ m_io_service->run(); }).detach();
  }

  return available;
}

bool Peer::RunCommand(Command command) {
  if (available) {
    m_socket->async_send(asio::buffer(command.command), asio::use_future);
    available = false;
    return true;
  }

  return false;
}

void Peer::ReadResponse()
{
  asio::async_read_until(*m_socket, response_buff, '\n',
    [&](const asio::error_code& ec, size_t length){
    if (ec) {
      return;
    }
    std::string idstr;
    std::string statusstr;
    std::string blob;
    int id = -1;
    int status;
    try {
      std::istream response_stream(&response_buff);

      idstr = ReadUntilDelim(response_stream, '|');
      id = stoi(idstr.substr(2));
      statusstr = ReadUntilDelim(response_stream, '|');
      status = stoi(statusstr.substr(2));

      Response resp(id, status, blob);
      command_runner->ReceiveResponse(resp);
      ReadResponse();
    }
    catch (...) {
      if (id >= 0) {
        Response resp(id, -1, "ERROR!");
        command_runner->ReceiveResponse(resp);
      }
    }
  });
}


PeerCommandRunner::PeerCommandRunner(const BuildConfig& config) 
  :config_(config) {

}

vector<Edge*> PeerCommandRunner::GetActiveEdges() {
  vector<Edge*> edges;
  for (auto e = id_to_edge_.begin(); e != id_to_edge_.end(); ++e)
    edges.push_back(e->second);
  return edges;
}

void PeerCommandRunner::Abort() {
  
}

bool PeerCommandRunner::CanRunMore() {
  for (Peer* peer : peers_) {
    if (peer->available) {
      return true;
    }
  }

  return false;
}

bool PeerCommandRunner::StartCommand(Edge* edge) {
  int id = g_IdCounter++;
  Command command(g_IdCounter++, edge->EvaluateCommand(), "");
  for (Peer* peer : peers_) {
    if (peer->available) {
      if (peer->RunCommand(command)) {
        id_to_edge_.insert(make_pair(id, edge));
        return true;
      }
    }
  }

  return false;
}

bool PeerCommandRunner::WaitForCommand(Result* result) {
  
  if (responses_.size() == 0) {
    return false;
  }

  Response resp = responses_.front();
  result->status = resp.status  == 0 ? ExitSuccess : ExitFailure;
  result->output = resp.message;
  responses_.pop();

  map<int, Edge*>::iterator e = id_to_edge_.find(resp.id);
  result->edge = e->second;
  id_to_edge_.erase(e);

  return true;
}

vector<string> PeerCommandRunner::GetPeerAddresses() {
  vector<string> peers;

  peers.push_back("127.0.0.1");

  return peers;
}

void PeerCommandRunner::ReceiveResponse(Response resp) {
  responses_.push(resp);
}