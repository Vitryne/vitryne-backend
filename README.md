<div align="center">

<img src="https://raw.githubusercontent.com/Vitryne/.github/main/assets/logotipoGradiente.png" width="280" alt="Logo" />

# Backend da plataforma Vitryne

API responsável pela autenticação, regras de negócio e persistência de dados da plataforma Vitryne

[![Java](https://skillicons.dev/icons?i=java,spring,postgres,docker)](https://skillicons.dev)

</div>

---

## 📖 Visão Geral

O backend da Vitryne expõe as APIs consumidas pelas aplicações web e mobile, centralizando regras de negócio, autenticação e acesso aos dados.

---

## 🛠️ Stack

- Java 21
- Spring Boot
- Maven
- Flyway
- PostgreSQL

---

## 🚀 Começando

### Pré-requisitos

- [Java 21+](https://docs.oracle.com/en/java/)
- [Docker e Docker Compose](https://www.docker.com/)

---

### Executando

```bash
# Clone o repositório
git clone https://github.com/Vitryne/backend.git
cd backend

# Crie o arquivo .env na raiz (veja a seção Variáveis de Ambiente)

# Suba os containers (aplicação + banco)
docker compose up --build
```

---

## ⚙️ Variáveis de Ambiente

Crie um arquivo `.env` na raiz do projeto. Você consegue verificar as variáveis que são utilizadas hoje, no arquivo `.env.example`.

---

## 📚 Documentação

A documentação da plataforma, incluindo arquitetura, regras de negócio e decisões técnicas, está disponível no repositório de documentação.

## 🌐 Ecossistema

| Repositório | Descrição |
| ------------- | ----------- |
| [Backend](https://github.com/Vitryne/backend) | API da plataforma |
| [Frontend](https://github.com/Vitryne/frontend) | Aplicação Web |
| [Mobile](https://github.com/Vitryne/mobile) | Aplicativo Mobile |
| [Docs](https://github.com/Vitryne/docs) | Documentação técnica |

## 📄 Licença

Este projeto está licenciado sob a licença MIT. Consulte o arquivo [LICENSE](LICENSE) para mais informações.
